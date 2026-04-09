import sharp from "sharp";
import Tesseract from "tesseract.js";
import { ScheduleParser } from "./types";

/** City keyword → regionId mapping for DTEK Telegram channel. */
const CITY_KEYWORDS: Record<string, string> = {
  "Київ": "kyiv",
  "Одес": "odesa",
  "Дніпро": "dnipro",
};

/**
 * Parser for ДТЕК official Telegram channel (t.me/dtek_ua).
 *
 * DTEK publishes schedule images with 3 queues per row:
 *   Row 1: "1.1 Черга   1.2 Черга   2.1 Черга"
 *   Row 2: "Світло буде відсутнє ..."
 *   Row 3: "з 10:50 до 14:00   з 10:50 до 11:00   з 21:00 до 22:00"
 *
 * Each city gets 2 images: queues 1.1–3.2 and 4.1–6.2.
 */
export class DtekTelegramParser implements ScheduleParser {
  constructor(private readonly targetRegionId: string) {}

  parse(_data: string | Buffer): [number, number][] {
    return [];
  }

  /** Fetch DTEK channel, OCR schedule images, return per-queue intervals. */
  async parseChannelAsync(): Promise<Map<string, [number, number][]>> {
    const html = await fetchChannelHtml();
    const imageUrls = extractImageUrls(html);
    const result = new Map<string, [number, number][]>();

    for (const url of imageUrls) {
      try {
        const raw = await fetchImage(url);
        if (raw.length < 15000) continue;

        const text = await ocrImage(raw);
        if (!isScheduleImage(text)) continue;
        if (detectRegion(text) !== this.targetRegionId) continue;

        const queues = parseDtekScheduleText(text);
        for (const [queueId, intervals] of queues) {
          const list = result.get(queueId) ?? [];
          list.push(...intervals);
          result.set(queueId, list);
        }
      } catch {
        // Skip unprocessable images
      }
    }

    return result;
  }
}

async function fetchChannelHtml(): Promise<string> {
  const res = await fetch("https://t.me/s/dtek_ua", {
    headers: { "User-Agent": "LightChecker-Functions/1.0" },
    signal: AbortSignal.timeout(15000),
  });
  return res.text();
}

function extractImageUrls(html: string): string[] {
  const re = /https:\/\/cdn[^"'\s)]+\.(?:jpg|jpeg|png|webp)/gi;
  const urls: string[] = [];
  let m: RegExpExecArray | null;
  while ((m = re.exec(html)) !== null) {
    if (!m[0].includes("emoji") && !m[0].includes("profile")) {
      urls.push(m[0]);
    }
  }
  return urls;
}

async function fetchImage(url: string): Promise<Buffer> {
  const res = await fetch(url, { signal: AbortSignal.timeout(15000) });
  return Buffer.from(await res.arrayBuffer());
}

async function ocrImage(raw: Buffer): Promise<string> {
  const processed = await sharp(raw)
    .greyscale()
    .normalize()
    .sharpen({ sigma: 2 })
    .threshold(128)
    .resize({ width: 2000, withoutEnlargement: false })
    .png()
    .toBuffer();

  const {
    data: { text },
  } = await Tesseract.recognize(processed, "ukr+eng", {
    logger: () => {},
  });
  return text;
}

function isScheduleImage(text: string): boolean {
  return /черга/i.test(text) && /\d{1,2}:\d{2}/.test(text);
}

function detectRegion(text: string): string | null {
  for (const [keyword, regionId] of Object.entries(CITY_KEYWORDS)) {
    if (text.includes(keyword)) return regionId;
  }
  return null;
}

/**
 * Parse DTEK schedule OCR text.
 *
 * Layout: rows of 3 queues each:
 *   "1.1 Черга   1.2 Черга   2.1 Черга"
 *   "Світло буде відсутнє ..."
 *   "огг 310:50 до 14:00   or 310:50 до 11:00   orr 3 21:00 до 22:00"
 */
function parseDtekScheduleText(
  text: string,
): Map<string, [number, number][]> {
  const result = new Map<string, [number, number][]>();
  const lines = text.split(/\n/).map((l) => l.trim()).filter(Boolean);

  for (let i = 0; i < lines.length; i++) {
    // Find lines with queue headers: "1.1 Черга  1.2 Черга  2.1 Черга"
    const queueIds = extractQueueIds(lines[i]);
    if (queueIds.length === 0) continue;

    // Look ahead 1-3 lines for time data
    const timeLine = findTimeLine(lines, i + 1, 3);
    if (!timeLine) continue;

    // Extract all time ranges from the time line
    const allRanges = extractAllTimeRanges(timeLine);

    // Match queues to ranges positionally
    // If we have N queues and N ranges — 1:1 mapping
    // If ranges < queues — some queues have no outage
    if (allRanges.length > 0) {
      if (allRanges.length === queueIds.length) {
        for (let j = 0; j < queueIds.length; j++) {
          const list = result.get(queueIds[j]) ?? [];
          list.push(allRanges[j]);
          result.set(queueIds[j], list);
        }
      } else {
        // Can't reliably map — assign all ranges to all queues
        // Better: try positional matching by character offset
        assignByPosition(timeLine, queueIds, result);
      }
    }
  }

  return result;
}

/** Extract queue IDs like "1.1", "2.1" from a line. */
function extractQueueIds(line: string): string[] {
  const re = /(\d+)[.,\s]*(\d+)\s*Черг/gi;
  const ids: string[] = [];
  let m: RegExpExecArray | null;
  while ((m = re.exec(line)) !== null) {
    ids.push(`${m[1]}.${m[2]}`);
  }
  return ids;
}

/** Find the next line that contains time data (within maxLookahead lines). */
function findTimeLine(
  lines: string[],
  startIdx: number,
  maxLookahead: number,
): string | null {
  for (let i = startIdx; i < Math.min(lines.length, startIdx + maxLookahead); i++) {
    if (/\d{1,2}:\d{2}/.test(lines[i])) return lines[i];
  }
  return null;
}

/** Extract all "з HH:MM до HH:MM" or "HH:MM-HH:MM" ranges. */
function extractAllTimeRanges(text: string): [number, number][] {
  const results: [number, number][] = [];
  // DTEK OCR produces "3 10:50 до 14:00" or "з 07:00 до 10:30"
  const re = /(\d{1,2})[:.:](\d{2})\s*(?:до|по|go|no)\s*(\d{1,2})[:.:](\d{2})/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(text)) !== null) {
    const startMin = parseInt(m[1]) * 60 + parseInt(m[2]);
    const endMin = parseInt(m[3]) * 60 + parseInt(m[4]);
    if (startMin < endMin && endMin <= 1440) {
      results.push([startMin, endMin]);
    }
  }
  return results;
}

/**
 * Assign time ranges to queues based on character position in the line.
 * Split the line into roughly equal thirds and assign each range to the
 * queue whose third it falls in.
 */
function assignByPosition(
  timeLine: string,
  queueIds: string[],
  result: Map<string, [number, number][]>,
): void {
  if (queueIds.length === 0) return;
  const segmentWidth = timeLine.length / queueIds.length;
  const re = /(\d{1,2})[:.:](\d{2})\s*(?:до|по|go|no)\s*(\d{1,2})[:.:](\d{2})/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(timeLine)) !== null) {
    const startMin = parseInt(m[1]) * 60 + parseInt(m[2]);
    const endMin = parseInt(m[3]) * 60 + parseInt(m[4]);
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
