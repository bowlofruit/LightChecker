import { kyivTodayYyyymmdd, kyivTomorrowYyyymmdd } from "../kyivDate";
import { extractLvivScheduleDayYyyymmdd } from "../lvivGraphicDate";
import { collectLvivScheduleCandidates } from "../lvivTelegramCandidates";
import { fetchImage } from "./imageFetch";
import { ocrImage } from "./ocrImage";
import { ScheduleParser } from "./types";

/** Результат парсингу Львова: черги + дата з картинки (ДД.ММ), якщо OCR її знайшов. */
export type LvivScheduleParseResult = {
  scheduleDayYyyymmdd: number | null;
  queues: Map<string, [number, number][]>;
  /** Який пост дав прийнятий графік (для логів). */
  usedPostId?: string;
};

export { extractLvivScheduleDayYyyymmdd } from "../lvivGraphicDate";
export {
  collectLvivScheduleCandidates,
  lvivCandidatesFingerprint,
  LVIV_MAX_SCHEDULE_CANDIDATES,
} from "../lvivTelegramCandidates";
export type { LvivScheduleCandidate } from "../lvivTelegramCandidates";

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

  async parseChannelAsync(): Promise<LvivScheduleParseResult> {
    const html = await fetchLvivChannelHtml();
    return this.parseFromHtml(html);
  }

  /**
   * OCR від новішого поста: перший графік, у якого дата з картинки — сьогодні або завтра (Kyiv).
   */
  async parseFromHtml(
    html: string,
    now: Date = new Date(),
  ): Promise<LvivScheduleParseResult> {
    const todayN = kyivTodayYyyymmdd(now);
    const tomorrowN = kyivTomorrowYyyymmdd(now);
    const allowed = new Set([String(todayN), String(tomorrowN)]);

    const candidates = collectLvivScheduleCandidates(html);
    if (candidates.length === 0) {
      return { scheduleDayYyyymmdd: null, queues: new Map() };
    }

    for (const { postId, imageUrl } of candidates) {
      try {
        const raw = await fetchImage(imageUrl);
        const text = await ocrImage(raw);
        const scheduleDayYyyymmdd = extractLvivScheduleDayYyyymmdd(text, now);
        if (scheduleDayYyyymmdd == null) continue;
        if (!allowed.has(String(scheduleDayYyyymmdd))) continue;

        const queues = parseLvivScheduleText(text);
        if (queues.size === 0) continue;

        return { scheduleDayYyyymmdd, queues, usedPostId: postId };
      } catch {
        // наступний кандидат
      }
    }

    return { scheduleDayYyyymmdd: null, queues: new Map() };
  }
}

export async function fetchLvivChannelHtml(): Promise<string> {
  const res = await fetch("https://t.me/s/lvivoblenergo", {
    headers: { "User-Agent": "LightChecker-Functions/1.0" },
    signal: AbortSignal.timeout(15000),
  });
  return res.text();
}

/**
 * Parse Lviv schedule text. Each row has 2 queues side by side:
 *   "1.1          1.2"
 *   "Електроенергії немає  Електроенергії немає"
 *   "з 11:30 по 14:00      з 20:00 по 22:00"
 *   (optional extra time line for queues with multiple intervals)
 */
export function parseLvivScheduleText(
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

    // Рядок статусу без годин — лише контекст для OCR, інтервалів немає
    const timeRanges = extractTimeRanges(line);
    if (timeRanges.length === 0) continue;

    // Time line (може бути разом із «Електроенергія є / немає») — колонки по позиції в рядку
    if (currentQueues.length > 0) {
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

    const matchCenter = m.index + m[0].length / 2;
    const segmentIdx = Math.min(
      Math.floor(matchCenter / segmentWidth),
      queueIds.length - 1,
    );
    const queueId = queueIds[segmentIdx];
    result.get(queueId)?.push([startMin, endMin]);
  }
}
