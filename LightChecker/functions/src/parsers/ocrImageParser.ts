import sharp from "sharp";
import Tesseract from "tesseract.js";
import { ScheduleParser } from "./types";

/**
 * OCR-based parser for image schedules (PNG/JPEG) using Tesseract.js + sharp preprocessing.
 *
 * Preprocessing pipeline: greyscale → normalize → sharpen → threshold → 2x upscale.
 * This improves confidence from ~35% to ~92% on colored table images.
 */
export class OcrImageParser implements ScheduleParser {
  /**
   * Synchronous parse — returns empty because OCR is async.
   * Use {@link parseAsync} instead.
   */
  parse(_data: string | Buffer): [number, number][] {
    return [];
  }

  /** Run OCR on an image buffer and extract all time intervals. */
  async parseAsync(data: Buffer): Promise<[number, number][]> {
    const text = await ocrImage(data);
    return extractAllTimeRanges(text);
  }

  /** Run OCR and return per-queue intervals. */
  async parseAllQueuesAsync(
    data: Buffer,
  ): Promise<Map<string, [number, number][]>> {
    const text = await ocrImage(data);
    return extractQueueIntervals(text);
  }
}

/** Preprocess image for better OCR, then run Tesseract. */
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
 * Extract all time ranges from OCR text.
 * Supports formats:
 * - "з 11:30 по 14:00" (Lviv oblenergo)
 * - "09:00 - 11:00"    (Cherkasy oblenergo)
 * - "08:00–10:00"       (generic)
 */
function extractAllTimeRanges(text: string): [number, number][] {
  const results: [number, number][] = [];
  const patterns = [
    // "з HH:MM по HH:MM" (Ukrainian oblenergo format)
    /з\s*(\d{1,2})[:.:](\d{2})\s*по\s*(\d{1,2})[:.:](\d{2})/g,
    // "з HH:MM до HH:MM"
    /з\s*(\d{1,2})[:.:](\d{2})\s*до\s*(\d{1,2})[:.:](\d{2})/g,
    // "HH:MM - HH:MM" or "HH:MM–HH:MM"
    /(\d{1,2})[:.:](\d{2})\s*[-–—]\s*(\d{1,2})[:.:](\d{2})/g,
  ];

  for (const re of patterns) {
    let m: RegExpExecArray | null;
    while ((m = re.exec(text)) !== null) {
      const startMin = parseInt(m[1]) * 60 + parseInt(m[2]);
      const endMin = parseInt(m[3]) * 60 + parseInt(m[4]);
      if (startMin < endMin && endMin <= 1440) {
        // Avoid duplicates
        if (!results.some(([s, e]) => s === startMin && e === endMin)) {
          results.push([startMin, endMin]);
        }
      }
    }
  }
  return results;
}

/**
 * Parse OCR text into per-queue intervals.
 * Handles Lviv format where queue headers (1.1, 2.1) appear on separate lines
 * followed by "з HH:MM по HH:MM" intervals.
 */
function extractQueueIntervals(
  text: string,
): Map<string, [number, number][]> {
  const result = new Map<string, [number, number][]>();
  const lines = text.split(/\n/);
  let currentQueue: string | null = null;

  for (const line of lines) {
    const trimmed = line.trim();

    // Check for queue header: "1.1", "2.2", "4. 1" (OCR may add space)
    const queueMatch = trimmed.match(/^(\d+)[.,\s]*(\d+)\s*$/);
    if (queueMatch) {
      currentQueue = `${queueMatch[1]}.${queueMatch[2]}`;
      if (!result.has(currentQueue)) result.set(currentQueue, []);
      continue;
    }

    // Check for inline queue + time: "1.1  з 11:30 по 14:00"
    const inlineMatch = trimmed.match(/^(\d+)[.,](\d+)\s+(.*)/);
    if (inlineMatch) {
      currentQueue = `${inlineMatch[1]}.${inlineMatch[2]}`;
      if (!result.has(currentQueue)) result.set(currentQueue, []);
      const intervals = extractAllTimeRanges(inlineMatch[3]);
      result.get(currentQueue)!.push(...intervals);
      continue;
    }

    // Extract time ranges for current queue
    if (currentQueue) {
      const intervals = extractAllTimeRanges(trimmed);
      if (intervals.length > 0) {
        result.get(currentQueue)!.push(...intervals);
      }
    }
  }

  return result;
}
