import sharp from "sharp";
import Tesseract from "tesseract.js";
import { ScheduleParser } from "./types";

/**
 * Parser for Львівобленерго Telegram channel (t.me/lvivoblenergo).
 *
 * Schedule posts have NO text — only an image with a table.
 * Strategy: find the latest post with no text + image → OCR → parse.
 */
export class LvivTelegramParser implements ScheduleParser {
  parse(_data: string | Buffer): [number, number][] {
    return [];
  }

  /** Fetch channel, find latest schedule image, OCR it, return per-queue intervals. */
  async parseChannelAsync(): Promise<Map<string, [number, number][]>> {
    const html = await fetchChannelHtml();
    const imageUrl = findLatestScheduleImage(html);
    if (!imageUrl) return new Map();

    const raw = await fetchImage(imageUrl);
    const text = await ocrImage(raw);
    return extractQueueIntervals(text);
  }
}

async function fetchChannelHtml(): Promise<string> {
  const res = await fetch("https://t.me/s/lvivoblenergo", {
    headers: { "User-Agent": "LightChecker-Functions/1.0" },
    signal: AbortSignal.timeout(15000),
  });
  return res.text();
}

/** Find the latest post with no text but with a large image = schedule. */
function findLatestScheduleImage(html: string): string | null {
  const blocks = html.split("tgme_widget_message_wrap");
  let latestImgUrl: string | null = null;

  for (const block of blocks) {
    if (!block.includes("data-post=")) continue;

    // Check if post has text content
    const textMatch = block.match(
      /tgme_widget_message_text[^>]*>([\s\S]*?)<\/div>/,
    );
    const text = textMatch
      ? textMatch[1].replace(/<[^>]+>/g, "").trim()
      : "";

    // Schedule posts have no text
    if (text.length > 10) continue;

    // Find non-avatar images
    const imgRe = /https:\/\/cdn[^"'\s)]+\.(?:jpg|jpeg|png|webp)/gi;
    let im: RegExpExecArray | null;
    while ((im = imgRe.exec(block)) !== null) {
      if (!im[0].includes("emoji") && !im[0].includes("user_photo")) {
        latestImgUrl = im[0]; // Keep overwriting — last one is most recent
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

/** Parse Lviv schedule — queue headers on separate lines, "з HH:MM по HH:MM". */
function extractQueueIntervals(
  text: string,
): Map<string, [number, number][]> {
  const result = new Map<string, [number, number][]>();
  const lines = text.split(/\n/);
  let currentQueue: string | null = null;

  for (const line of lines) {
    const trimmed = line.trim();

    // Queue header: "1.1" or "4. 1" or "1.1  1.2" (two on one line)
    const queueHeaders = trimmed.match(/(\d+)[.,\s]*(\d+)/g);
    if (
      queueHeaders &&
      trimmed.length < 20 &&
      !/\d{1,2}:\d{2}/.test(trimmed)
    ) {
      // Use first queue on line as current
      const m = trimmed.match(/(\d+)[.,\s]*(\d+)/);
      if (m) {
        currentQueue = `${m[1]}.${m[2]}`;
        if (!result.has(currentQueue)) result.set(currentQueue, []);
      }
      continue;
    }

    // Time ranges for current queue
    if (currentQueue) {
      const ranges = extractTimeRanges(trimmed);
      if (ranges.length > 0) {
        result.get(currentQueue)!.push(...ranges);
      }
    }
  }

  return result;
}

function extractTimeRanges(text: string): [number, number][] {
  const results: [number, number][] = [];
  const patterns = [
    /з\s*(\d{1,2})[:.:](\d{2})\s*(?:по|до)\s*(\d{1,2})[:.:](\d{2})/g,
    /(\d{1,2})[:.:](\d{2})\s*[-–—]\s*(\d{1,2})[:.:](\d{2})/g,
  ];
  for (const re of patterns) {
    let m: RegExpExecArray | null;
    while ((m = re.exec(text)) !== null) {
      const startMin = parseInt(m[1]) * 60 + parseInt(m[2]);
      const endMin = parseInt(m[3]) * 60 + parseInt(m[4]);
      if (startMin < endMin && endMin <= 1440) {
        if (!results.some(([s, e]) => s === startMin && e === endMin)) {
          results.push([startMin, endMin]);
        }
      }
    }
  }
  return results;
}
