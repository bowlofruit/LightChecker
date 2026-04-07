import { ScheduleParser } from "./types";

/**
 * Parser for АТ "Черкасиобленерго" Telegram channel (t.me/pat_cherkasyoblenergo).
 *
 * Supports two post formats:
 *
 * 1. Per-subqueue (ГПВ):
 *    "4.1 18:00 - 20:00"
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

    // Pattern 1: per-subqueue lines — "4.1 18:00 - 20:00"
    const queueLineRe =
      /(\d+\.\d+)\s+(\d{1,2}):(\d{2})\s*[-–]\s*(\d{1,2}):(\d{2})/g;
    let match: RegExpExecArray | null;
    let foundQueueLines = false;

    while ((match = queueLineRe.exec(text)) !== null) {
      foundQueueLines = true;
      const queueId = match[1];
      if (this.targetQueueId && queueId !== this.targetQueueId) continue;

      const startMin = parseInt(match[2]) * 60 + parseInt(match[3]);
      const endMin = parseInt(match[4]) * 60 + parseInt(match[5]);
      if (startMin <= endMin && endMin <= 1440) {
        pairs.push([startMin, endMin]);
      }
    }

    if (foundQueueLines) return pairs;

    // Pattern 2: general "з HH:MM до HH:MM" (ГОП format)
    const gopRe = /з\s+(\d{1,2}):(\d{2})\s+до\s+(\d{1,2}):(\d{2})/g;
    while ((match = gopRe.exec(text)) !== null) {
      const startMin = parseInt(match[1]) * 60 + parseInt(match[2]);
      const endMin = parseInt(match[3]) * 60 + parseInt(match[4]);
      if (startMin <= endMin && endMin <= 1440) {
        pairs.push([startMin, endMin]);
      }
    }

    return pairs;
  }
}

/**
 * Parse a full Telegram post and return a map of queueId → intervals.
 * Useful for batch-processing a single post for all queues at once.
 */
export function parseAllQueues(
  text: string,
): Map<string, [number, number][]> {
  const result = new Map<string, [number, number][]>();
  const re =
    /(\d+\.\d+)\s+(\d{1,2}):(\d{2})\s*[-–]\s*(\d{1,2}):(\d{2})/g;
  let match: RegExpExecArray | null;

  while ((match = re.exec(text)) !== null) {
    const queueId = match[1];
    const startMin = parseInt(match[2]) * 60 + parseInt(match[3]);
    const endMin = parseInt(match[4]) * 60 + parseInt(match[5]);
    if (startMin <= endMin && endMin <= 1440) {
      const list = result.get(queueId) ?? [];
      list.push([startMin, endMin]);
      result.set(queueId, list);
    }
  }

  return result;
}
