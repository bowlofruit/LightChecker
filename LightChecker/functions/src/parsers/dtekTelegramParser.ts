import { extractDtekScheduleDayYyyymmdd } from "../dtekPostDate";
import { fetchImage } from "./imageFetch";
import { ocrImage } from "./ocrImage";
import { ScheduleParser } from "./types";

export type DtekScheduleParseResult = {
  scheduleDayYyyymmdd: number | null;
  queues: Map<string, [number, number][]>;
};

export { extractDtekScheduleDayYyyymmdd, sliceDtekTextForRegion } from "../dtekPostDate";

/** Post text keyword → regionId. */
const REGION_KEYWORDS: Record<string, string> = {
  "Київщин": "kyiv",
  "Одещин": "odesa",
  "Дніпропетровщин": "dnipro",
};

export interface ChannelPost {
  postId: string;
  text: string;
  imageUrls: string[];
}

const DTEK_CHANNEL = "dtek_ua";

/** Fingerprint for the latest schedule post for a region (same postId → same images). */
export function fingerprintDtekForRegion(
  posts: ChannelPost[],
  targetRegionId: string,
): string | null {
  const targetPosts = posts.filter((p) => {
    if (!/графік|відключен/i.test(p.text)) return false;
    const regionId = detectRegionFromText(p.text);
    return regionId === targetRegionId;
  });
  if (targetPosts.length === 0) return null;
  const latest = targetPosts[targetPosts.length - 1];
  return `${DTEK_CHANNEL}/${latest.postId}`;
}

/**
 * Parser for ДТЕК official Telegram channel (t.me/dtek_ua).
 *
 * Strategy:
 * 1. Fetch channel HTML, split into posts
 * 2. Identify city by POST TEXT ("Київщина:", "Одещина:", "Дніпропетровщина:")
 * 3. OCR only the schedule images from matching posts (skip avatar = img1)
 * 4. Parse time intervals from OCR text
 */
export class DtekTelegramParser implements ScheduleParser {
  constructor(private readonly targetRegionId: string) {}

  parse(_data: string | Buffer): [number, number][] {
    return [];
  }

  /** Fetch DTEK channel, find posts for target city, OCR images, return per-queue intervals. */
  async parseChannelAsync(): Promise<DtekScheduleParseResult> {
    const html = await fetchDtekChannelHtml();
    const posts = parseDtekChannelPosts(html);
    return this.parseFromPosts(posts);
  }

  /** Reuse already-fetched channel HTML (e.g. one fetch for Kyiv/Odesa/Dnipro). */
  async parseFromPosts(
    posts: ChannelPost[],
  ): Promise<DtekScheduleParseResult> {
    const result = new Map<string, [number, number][]>();

    const targetPosts = posts.filter((p) => {
      if (!/графік|відключен/i.test(p.text)) return false;
      const regionId = detectRegionFromText(p.text);
      return regionId === this.targetRegionId;
    });

    if (targetPosts.length === 0) {
      return { scheduleDayYyyymmdd: null, queues: result };
    }

    const latest = targetPosts[targetPosts.length - 1];
    const scheduleDayYyyymmdd = extractDtekScheduleDayYyyymmdd(
      latest.text,
      this.targetRegionId,
    );

    const scheduleImages = latest.imageUrls.filter(
      (url) => !url.includes("user_photo"),
    );
    const imagesToOcr =
      scheduleImages.length > 2 ? scheduleImages.slice(1) : scheduleImages;

    for (const url of imagesToOcr) {
      try {
        const raw = await fetchImage(url);
        if (raw.length < 15000) continue;

        const text = await ocrImage(raw);
        const queues = parseDtekScheduleText(text);
        for (const [queueId, intervals] of queues) {
          const list = result.get(queueId) ?? [];
          list.push(...intervals);
          result.set(queueId, list);
        }
      } catch {
        continue;
      }
    }

    return { scheduleDayYyyymmdd, queues: result };
  }
}

/** Fetch t.me/s/dtek_ua public preview page. */
export async function fetchDtekChannelHtml(): Promise<string> {
  const res = await fetch("https://t.me/s/dtek_ua", {
    headers: { "User-Agent": "LightChecker-Functions/1.0" },
    signal: AbortSignal.timeout(15000),
  });
  return res.text();
}

/** Parse channel HTML into structured posts. */
export function parseDtekChannelPosts(html: string): ChannelPost[] {
  const posts: ChannelPost[] = [];
  const blocks = html.split("tgme_widget_message_wrap");

  for (const block of blocks) {
    const postMatch = block.match(/data-post="dtek_ua\/(\d+)"/);
    if (!postMatch) continue;

    const textMatch = block.match(
      /tgme_widget_message_text[^>]*>([\s\S]*?)<\/div>/,
    );
    const text = textMatch
      ? textMatch[1]
          .replace(/<br\s*\/?>/gi, " ")
          .replace(/<[^>]+>/g, "")
          .trim()
      : "";

    const imageUrls: string[] = [];
    const imgRe = /https:\/\/cdn[^"'\s)]+\.(?:jpg|jpeg|png|webp)/gi;
    let im: RegExpExecArray | null;
    while ((im = imgRe.exec(block)) !== null) {
      if (!im[0].includes("emoji")) {
        imageUrls.push(im[0]);
      }
    }

    posts.push({ postId: postMatch[1], text, imageUrls });
  }

  return posts;
}

/** Detect region from post text (not OCR). */
function detectRegionFromText(text: string): string | null {
  for (const [keyword, regionId] of Object.entries(REGION_KEYWORDS)) {
    if (text.includes(keyword)) return regionId;
  }
  return null;
}

/**
 * Parse DTEK schedule OCR text — 3 queues per row:
 *   "1.1 Черга   1.2 Черга   2.1 Черга"
 *   "Світло буде відсутнє ..."
 *   "огг 310:50 до 14:00   or 310:50 до 11:00 ..."
 */
function parseDtekScheduleText(
  text: string,
): Map<string, [number, number][]> {
  const result = new Map<string, [number, number][]>();
  const lines = text
    .split(/\n/)
    .map((l) => l.trim())
    .filter(Boolean);

  for (let i = 0; i < lines.length; i++) {
    const queueIds = extractQueueIds(lines[i]);
    if (queueIds.length === 0) continue;

    const timeLine = findTimeLine(lines, i + 1, 3);
    if (!timeLine) continue;

    assignByPosition(timeLine, queueIds, result);
  }

  return result;
}

/**
 * Extract queue IDs from a line. Handles OCR artifacts:
 * "24 Черга" → "2.1" (OCR merges dot+1 into "4")
 * "34 Черга" → "3.1"
 */
function extractQueueIds(line: string): string[] {
  const re = /(\d)[.,\s]*(\d)\s*Черг/gi;
  const ids: string[] = [];
  let m: RegExpExecArray | null;
  while ((m = re.exec(line)) !== null) {
    let minor = m[2];
    if (parseInt(minor) > 2) minor = "1";
    ids.push(`${m[1]}.${minor}`);
  }
  return ids;
}

function findTimeLine(
  lines: string[],
  startIdx: number,
  maxLookahead: number,
): string | null {
  for (
    let i = startIdx;
    i < Math.min(lines.length, startIdx + maxLookahead);
    i++
  ) {
    if (/\d{1,2}[:.]\d{2}/.test(lines[i]) || /3\d{4,5}/.test(lines[i])) return lines[i];
  }
  return null;
}

/**
 * Assign time ranges to queues by character position.
 * The time line has N ranges spread across the width — each maps to the
 * queue whose "column" it falls in.
 */
function assignByPosition(
  timeLine: string,
  queueIds: string[],
  result: Map<string, [number, number][]>,
): void {
  if (queueIds.length === 0) return;

  const normalized = timeLine
    .replace(/\u041E\u041E/g, "00")
    .replace(/\u041E(?=\d|:)/g, "0")
    .replace(/3(\d{2})(\d{2})/g, "3 $1:$2")
    .replace(/3(\d{2})\.(\d{2})/g, "3 $1:$2")
    .replace(/(\d{2})\.(\d{2})/g, "$1:$2")
    .replace(/\bpo\b/gi, "до")
    .replace(/\bno\b/gi, "до")
    .replace(/\bgo\b/gi, "до")
    .replace(/з\s+(\d{1,2})\s+(до|по)/g, "з $1:00 $2");

  const segmentWidth = normalized.length / queueIds.length;
  const re =
    /(\d{1,2})[:.:](\d{2})\s*(?:до|по)\s*(\d{1,2})[:.:](\d{2})/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(normalized)) !== null) {
    const startMin = parseInt(m[1]) * 60 + parseInt(m[2]);
    let endMin = parseInt(m[3]) * 60 + parseInt(m[4]);
    if (endMin < startMin && endMin < 360) {
      endMin = parseInt(m[3] + "1") * 60 + parseInt(m[4]);
    }
    if (startMin >= endMin || endMin > 1440) continue;

    const segmentIdx = Math.min(
      Math.floor(m.index / segmentWidth),
      queueIds.length - 1,
    );
    const queueId = queueIds[segmentIdx];
    const list = result.get(queueId) ?? [];
    list.push([startMin, endMin]);
    result.set(queueId, list);
  }
}
