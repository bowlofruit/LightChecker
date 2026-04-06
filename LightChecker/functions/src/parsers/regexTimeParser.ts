import { ScheduleParser } from "./types";

/**
 * Regex-based parser for HH:mm-HH:mm time intervals.
 * Supports en-dash (\u2013), minus sign (\u2212), and regular hyphen (-) as separators.
 */
export class RegexTimeParser implements ScheduleParser {
  parse(data: string | Buffer): [number, number][] {
    const text = typeof data === "string" ? data : data.toString("utf-8");
    const re = /(\d{1,2}):(\d{2})\s*[\u2013\u2212-]\s*(\d{1,2}):(\d{2})/g;
    const out: [number, number][] = [];
    let m: RegExpExecArray | null;
    while ((m = re.exec(text)) !== null) {
      const start = Number(m[1]) * 60 + Number(m[2]);
      const end = Number(m[3]) * 60 + Number(m[4]);
      if (start >= 0 && end <= 1440 && start <= end) {
        out.push([start, end]);
      }
    }
    return out;
  }
}
