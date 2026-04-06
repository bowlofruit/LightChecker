import { ScheduleParser } from "./types";
import { RegexTimeParser } from "./regexTimeParser";
import { DtekHtmlParser } from "./dtekHtmlParser";
import { OcrImageParser } from "./ocrImageParser";

const parsers: Record<string, ScheduleParser> = {
  regex: new RegexTimeParser(),
  dtek_html: new DtekHtmlParser(),
  image_ocr: new OcrImageParser(),
};

export function getParser(name: string): ScheduleParser {
  const parser = parsers[name];
  if (!parser) throw new Error(`Unknown parser: ${name}`);
  return parser;
}

export type { ScheduleParser };
