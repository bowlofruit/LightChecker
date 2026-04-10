import sharp from "sharp";
import Tesseract from "tesseract.js";
import { ScheduleParser } from "./types";

/**
 * Parser for Львівобленерго Telegram channel (t.me/lvivoblenergo).
 *
 * Schedule posts have NO text — only an image with a table.
 * Layout: 2 queues per row, each with its own time intervals:
 *   "1.1          1.2"
 *   "Електроенергії немає  Електроенергії немає"
 *   "з 11:30 по 14:00      з 20:00 по 22:00"
 */
export class LvivTelegramParser implements ScheduleParser {
  parse(_data: string | Buffer): [number, number][] {
    return [];
  }

  async parseChannelAsync(): Promise<Map<string, [number, number][]>> {
    const html = await fetchChannelHtml();
    const imageUrl = findLatestScheduleImage(html);
    if (!imageUrl) return new Map();

    const raw = await fetchImage(imageUrl);
    const text = await ocrImage(raw);
    return parseLvivScheduleText(text);
  }
}

async function fetchChannelHtml(): Promise<string> {
  const res = await fetch("https://t.me/s/lvivoblenergo", {
    headers: { "User-Agent": "LightChecker-Functions/1.0" },
    signal: AbortSignal.timeout(15000),
  });
  return res.text();
}

function findLatestScheduleImage(html: string): string | null {
  const blocks = html.split("tgme_widget_message_wrap");
  let latestImgUrl: string | null = null;

  for (const block of blocks) {
    if (!block.includes("data-post=")) continue;
    const textMatch = block.match(
      /tgme_widget_message_text[^>]*>([\s\S]*?)<\/div>/,
    );
    const text = textMatch
      ? textMatch[1].replace(/<[^>]+>/g, "").trim()
      : "";
    if (text.length > 10) continue;

    const imgRe = /https:\/\/cdn[^"'\s)]+\.(?:jpg|jpeg|png|webp)/gi;
    let im: RegExpExecArray | null;
    while ((im = imgRe.exec(block)) !== null) {
      if (!im[0].includes("emoji") && !im[0].includes("user_photo")) {
        latestImgUrl = im[0];
      }
    }
  }

  return latestImgUrl;
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

/**
 * Parse Lviv schedule text. Each row has 2 queues side by side:
 *   "1.1          1.2"
 *   "Електроенергії немає  Електроенергії немає"
 *   "з 11:30 по 14:00      з 20:00 по 22:00"
 *   (optional extra time line for queues with multiple intervals)
 */
function parseLvivScheduleText(
  text: string,
): Map<string, [number, number][]> {
  const result = new Map<string, [number, number][]>();
  const lines = text
    .split(/\n/)
    .map((l) => l.trim())
    .filter(Boolean);

  let currentQueues: string[] = [];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Try to find queue header line: "1.1  1.2" or "4. 1  4.2"
    const queueIds = extractQueueIds(line);
    if (queueIds.length > 0) {
      currentQueues = queueIds;
      for (const q of queueIds) {
        if (!result.has(q)) result.set(q, []);
      }
      continue;
    }

    // Skip "Електроенергії немає" lines
    if (/електроенерг/i.test(line)) continue;

    // Time line — assign to current queues by position
    const timeRanges = extractTimeRanges(line);
    if (timeRanges.length > 0 && currentQueues.length > 0) {
      if (currentQueues.length >= 2 && timeRanges.length >= 2) {
        // 2 queues, 2+ ranges — split by position
        assignByPosition(line, currentQueues, result);
      } else if (timeRanges.length === 1 && currentQueues.length >= 2) {
        // 1 range — could belong to either queue; use position
        assignByPosition(line, currentQueues, result);
      } else {
        // Single queue or fallback
        const q = currentQueues[0];
        result.get(q)?.push(...timeRanges);
      }
    }
  }

  return result;
}

function extractQueueIds(line: string): string[] {
  // Match "1.1", "4. 1", "6.2" etc — must not contain time patterns
  if (/\d{1,2}:\d{2}/.test(line)) return [];
  if (/електроенерг/i.test(line)) return [];

  // Normalize OCR artifacts: "а.1" → "4.1" (Cyrillic "а" misread as digit "4")
  const normalized = line
    .replace(/а(?=[.,\s]*\d)/g, "4")
    .replace(/б(?=[.,\s]*\d)/g, "6");

  const re = /(\d+)[.,\s]+(\d+)/g;
  const ids: string[] = [];
  let m: RegExpExecArray | null;
  while ((m = re.exec(normalized)) !== null) {
    const major = parseInt(m[1]);
    const minor = parseInt(m[2]);
    if (major >= 1 && major <= 6 && minor >= 1 && minor <= 2) {
      ids.push(`${major}.${minor}`);
    }
  }
  return ids;
}

function extractTimeRanges(text: string): [number, number][] {
  const results: [number, number][] = [];
  // Normalize OCR: "3 07:00 no 10:00" → "з 07:00 до 10:00"
  const normalized = text
    .replace(/\bno\b/gi, "до")
    .replace(/\bpo\b/gi, "до")
    .replace(/\b3\s+(\d)/g, "з $1");

  const re =
    /з\s*(\d{1,2})[:.:](\d{2})\s*(?:по|до)\s*(\d{1,2})[:.:](\d{2})/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(normalized)) !== null) {
    const startMin = parseInt(m[1]) * 60 + parseInt(m[2]);
    const endMin = parseInt(m[3]) * 60 + parseInt(m[4]);
    if (startMin < endMin && endMin <= 1440) {
      results.push([startMin, endMin]);
    }
  }
  return results;
}

function assignByPosition(
  timeLine: string,
  queueIds: string[],
  result: Map<string, [number, number][]>,
): void {
  if (queueIds.length === 0) return;
  // Normalize before parsing
  const normalized = timeLine
    .replace(/\bno\b/gi, "до")
    .replace(/\bpo\b/gi, "до")
    .replace(/\b3\s+(\d)/g, "з $1");

  const segmentWidth = normalized.length / queueIds.length;
  const re =
    /з\s*(\d{1,2})[:.:](\d{2})\s*(?:по|до)\s*(\d{1,2})[:.:](\d{2})/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(normalized)) !== null) {
    const startMin = parseInt(m[1]) * 60 + parseInt(m[2]);
    const endMin = parseInt(m[3]) * 60 + parseInt(m[4]);
    if (startMin >= endMin || endMin > 1440) continue;

    const segmentIdx = Math.min(
      Math.floor(m.index / segmentWidth),
      queueIds.length - 1,
    );
    const queueId = queueIds[segmentIdx];
    result.get(queueId)?.push([startMin, endMin]);
  }
}
