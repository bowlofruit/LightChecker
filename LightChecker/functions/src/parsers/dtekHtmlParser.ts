import { RegexTimeParser } from "./regexTimeParser";
import { ScheduleParser } from "./types";

/** Stub parser for DTEK oblenergo HTML schedules. Falls back to regex. */
export class DtekHtmlParser implements ScheduleParser {
  parse(data: string | Buffer): [number, number][] {
    return new RegexTimeParser().parse(data);
  }
}
