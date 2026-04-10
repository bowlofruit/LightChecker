/**
 * Multi-city schedule update pipeline.
 *
 * Fetches schedules from all configured Telegram channels,
 * parses them (text or OCR), and writes to Firestore.
 */
import type { Firestore } from "firebase-admin/firestore";
import type { Messaging } from "firebase-admin/messaging";
import * as logger from "firebase-functions/logger";
import { parseAllQueues as cherkasyParseAll } from "./parsers/cherkasyTelegramParser";
import { DtekTelegramParser } from "./parsers/dtekTelegramParser";
import { LvivTelegramParser } from "./parsers/lvivTelegramParser";
import { kyivTodayYyyymmdd } from "./kyivDate";
import { applyScheduleUpdate } from "./schedulePipeline";

interface CityResult {
  regionId: string;
  queuesUpdated: number;
  queuesSkipped: number;
  error?: string;
}

/**
 * Run the full pipeline for all 5 cities.
 * Each city is processed independently — one failure doesn't block others.
 */
export async function updateAllCities(
  db: Firestore,
  messaging: Messaging,
): Promise<CityResult[]> {
  const day = kyivTodayYyyymmdd();
  const results: CityResult[] = [];

  // Cherkasy — text-based parser
  results.push(await processCherkasy(db, messaging, day));

  // DTEK cities — OCR-based parsers (Kyiv, Odesa, Dnipro)
  const dtekCities = [
    { regionId: "kyiv", label: "Kyiv" },
    { regionId: "odesa", label: "Odesa" },
    { regionId: "dnipro", label: "Dnipro" },
  ] as const;

  for (const city of dtekCities) {
    results.push(await processDtekCity(db, messaging, day, city.regionId, city.label));
  }

  // Lviv — OCR-based parser
  results.push(await processLviv(db, messaging, day));

  return results;
}

async function processCherkasy(
  db: Firestore,
  messaging: Messaging,
  day: number,
): Promise<CityResult> {
  try {
    const url = "https://t.me/s/pat_cherkasyoblenergo";
    const res = await fetch(url, {
      headers: { "User-Agent": "LightChecker-Functions/1.0" },
      signal: AbortSignal.timeout(15000),
    });
    const html = await res.text();

    // Extract text from Telegram HTML
    const msgRe = /<div class="tgme_widget_message_text[^"]*"[^>]*>([\s\S]*?)<\/div>/g;
    const messages: string[] = [];
    let m: RegExpExecArray | null;
    while ((m = msgRe.exec(html)) !== null) {
      const text = m[1]
        .replace(/<br\s*\/?>/gi, "\n")
        .replace(/<[^>]+>/g, "")
        .replace(/&amp;/g, "&")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&quot;/g, '"')
        .trim();
      messages.push(text);
    }

    // Find latest schedule message
    const scheduleMsg = messages
      .filter((msg) => /\d+\.\d+\s+\d{1,2}:\d{2}/.test(msg))
      .pop();

    if (!scheduleMsg) {
      return { regionId: "cherkasy", queuesUpdated: 0, queuesSkipped: 0, error: "no schedule found" };
    }

    const allQueues = cherkasyParseAll(scheduleMsg);
    let updated = 0;
    let skipped = 0;

    for (const [queueId, intervals] of allQueues) {
      const result = await applyScheduleUpdate(db, messaging, "cherkasy", queueId, day, intervals);
      if (result.skipped) skipped++;
      else updated++;
    }

    logger.info("cherkasy_done", { updated, skipped });
    return { regionId: "cherkasy", queuesUpdated: updated, queuesSkipped: skipped };
  } catch (e) {
    logger.error("cherkasy_error", e);
    return { regionId: "cherkasy", queuesUpdated: 0, queuesSkipped: 0, error: String(e) };
  }
}

async function processDtekCity(
  db: Firestore,
  messaging: Messaging,
  day: number,
  regionId: string,
  label: string,
): Promise<CityResult> {
  try {
    const parser = new DtekTelegramParser(regionId);
    const queues = await parser.parseChannelAsync();

    let updated = 0;
    let skipped = 0;

    for (const [queueId, intervals] of queues) {
      const result = await applyScheduleUpdate(db, messaging, regionId, queueId, day, intervals);
      if (result.skipped) skipped++;
      else updated++;
    }

    logger.info(`${label}_done`, { updated, skipped });
    return { regionId, queuesUpdated: updated, queuesSkipped: skipped };
  } catch (e) {
    logger.error(`${label}_error`, e);
    return { regionId, queuesUpdated: 0, queuesSkipped: 0, error: String(e) };
  }
}

async function processLviv(
  db: Firestore,
  messaging: Messaging,
  day: number,
): Promise<CityResult> {
  try {
    const parser = new LvivTelegramParser();
    const queues = await parser.parseChannelAsync();

    let updated = 0;
    let skipped = 0;

    for (const [queueId, intervals] of queues) {
      const result = await applyScheduleUpdate(db, messaging, "lviv", queueId, day, intervals);
      if (result.skipped) skipped++;
      else updated++;
    }

    logger.info("lviv_done", { updated, skipped });
    return { regionId: "lviv", queuesUpdated: updated, queuesSkipped: skipped };
  } catch (e) {
    logger.error("lviv_error", e);
    return { regionId: "lviv", queuesUpdated: 0, queuesSkipped: 0, error: String(e) };
  }
}
