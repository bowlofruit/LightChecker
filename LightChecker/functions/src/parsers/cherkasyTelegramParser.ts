import { ScheduleParser } from "./types";

/**
 * Parser for АТ "Черкасиобленерго" Telegram channel (t.me/pat_cherkasyoblenergo).
 *
 * Supports two post formats:
 *
 * 1. Per-subqueue (ГПВ) — one line per queue, may contain multiple intervals:
 *    "4.1 09:00 - 11:00, 15:00 - 17:00"
 *    "5.1 20:00 - 22:00"
 *
 * 2. General time range (ГОП):
 *    "з 07:00 до 10:00 та з 19:00 до 22:00"
 *
 * Returns intervals as minute pairs for a specific queueId (e.g. "4.1").
 */
export class CherkasyTelegramParser implements ScheduleParser {
  constructor(private readonly targetQueueId?: string) {}

  parse(data: string | Buffer): [number, number][] {
    const text = typeof data === "string" ? data : data.toString("utf-8");
    const pairs: [number, number][] = [];

    let foundQueueLines = false;
    for (const line of text.split(/\n/)) {
      const lineMatch = line.match(/^(\d+\.\d+)\s+(.+)/);
      if (!lineMatch) continue;
      foundQueueLines = true;
      const queueId = lineMatch[1];
      if (this.targetQueueId && queueId !== this.targetQueueId) continue;
      pairs.push(...extractTimeRanges(lineMatch[2]));
    }

    if (foundQueueLines) return pairs;

    const gopRe = /з\s+(\d{1,2}):(\d{2})\s+до\s+(\d{1,2}):(\d{2})/g;
    let gopMatch: RegExpExecArray | null;
    while ((gopMatch = gopRe.exec(text)) !== null) {
      const startMin = parseInt(gopMatch[1]) * 60 + parseInt(gopMatch[2]);
      const endMin = parseInt(gopMatch[3]) * 60 + parseInt(gopMatch[4]);
      if (startMin <= endMin && endMin <= 1440) {
        pairs.push([startMin, endMin]);
      }
    }

    return pairs;
  }
}

/** Extract all "HH:MM - HH:MM" ranges from a string fragment. */
function extractTimeRanges(fragment: string): [number, number][] {
  const re = /(\d{1,2}):(\d{2})\s*[-–]\s*(\d{1,2}):(\d{2})/g;
  const results: [number, number][] = [];
  let m: RegExpExecArray | null;
  while ((m = re.exec(fragment)) !== null) {
    const startMin = parseInt(m[1]) * 60 + parseInt(m[2]);
    const endMin = parseInt(m[3]) * 60 + parseInt(m[4]);
    if (startMin <= endMin && endMin <= 1440) {
      results.push([startMin, endMin]);
    }
  }
  return results;
}

/**
 * Parse a full Telegram post and return a map of queueId → intervals.
 * Useful for batch-processing a single post for all queues at once.
 */
export function parseAllQueues(
  text: string,
): Map<string, [number, number][]> {
  const result = new Map<string, [number, number][]>();
  for (const line of text.split(/\n/)) {
    const lineMatch = line.match(/^(\d+\.\d+)\s+(.+)/);
    if (!lineMatch) continue;
    const queueId = lineMatch[1];
    const intervals = extractTimeRanges(lineMatch[2]);
    const list = result.get(queueId) ?? [];
    list.push(...intervals);
    result.set(queueId, list);
  }

  return result;
}
