import { ScheduleParser } from "./types";
import { RegexTimeParser } from "./regexTimeParser";
import { DtekHtmlParser } from "./dtekHtmlParser";
import { OcrImageParser } from "./ocrImageParser";
import { CherkasyTelegramParser } from "./cherkasyTelegramParser";
import { DtekTelegramParser } from "./dtekTelegramParser";
import { LvivTelegramParser } from "./lvivTelegramParser";

const parsers: Record<string, ScheduleParser> = {
  regex: new RegexTimeParser(),
  dtek_html: new DtekHtmlParser(),
  image_ocr: new OcrImageParser(),
  cherkasy_telegram: new CherkasyTelegramParser(),
  dtek_kyiv: new DtekTelegramParser("kyiv"),
  dtek_odesa: new DtekTelegramParser("odesa"),
  dtek_dnipro: new DtekTelegramParser("dnipro"),
  lviv_telegram: new LvivTelegramParser(),
};

export function getParser(name: string): ScheduleParser {
  const parser = parsers[name];
  if (!parser) throw new Error(`Unknown parser: ${name}`);
  return parser;
}

export type { ScheduleParser };
