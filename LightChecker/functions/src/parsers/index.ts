import { ScheduleParser } from "./types";
import { RegexTimeParser } from "./regexTimeParser";
import { DtekHtmlParser } from "./dtekHtmlParser";
import { OcrImageParser } from "./ocrImageParser";
import { CherkasyTelegramParser } from "./cherkasyTelegramParser";

const parsers: Record<string, ScheduleParser> = {
  regex: new RegexTimeParser(),
  dtek_html: new DtekHtmlParser(),
  image_ocr: new OcrImageParser(),
  cherkasy_telegram: new CherkasyTelegramParser(),
};

export function getParser(name: string): ScheduleParser {
  const parser = parsers[name];
  if (!parser) throw new Error(`Unknown parser: ${name}`);
  return parser;
}

export type { ScheduleParser };
