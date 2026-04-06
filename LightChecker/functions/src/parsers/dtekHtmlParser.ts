import { ScheduleParser } from "./types";

/** Stub parser for DTEK oblenergo HTML schedules. To be implemented with real DOM parsing. */
export class DtekHtmlParser implements ScheduleParser {
  parse(data: string | Buffer): [number, number][] {
    // TODO: Implement actual HTML table parsing for DTEK format
    // For now, falls back to regex time parser
    const { RegexTimeParser } = require("./regexTimeParser");
    return new RegexTimeParser().parse(data);
  }
}
