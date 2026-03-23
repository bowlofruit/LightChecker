/**
 * Cloud Functions: health, cron pipeline (oblenergo stub → Firestore → FCM), опційний HTTP-тригер.
 */
import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import * as logger from "firebase-functions/logger";
import { onRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { fetchOblenergoStub } from "./fetchOblenergoStub";
import { kyivTodayYyyymmdd, kyivTomorrowYyyymmdd } from "./kyivDate";
import {
  httpScheduleKey,
  oblenergoSourceUrl,
  pipelineDayOffset,
  pipelineQueueId,
  pipelineRegionId,
} from "./params";
import { parseScheduleHtmlStub } from "./parseScheduleHtmlStub";
import { applyScheduleUpdate } from "./schedulePipeline";

if (!getApps().length) {
  initializeApp();
}

function effectivePipelineDayYyyymmdd(): number {
  return pipelineDayOffset.value().trim() === "1"
    ? kyivTomorrowYyyymmdd()
    : kyivTodayYyyymmdd();
}

export const healthCheck = onRequest((req, res) => {
  logger.info("healthCheck");
  res.status(200).json({
    ok: true,
    kyivToday: kyivTodayYyyymmdd(),
    kyivTomorrow: kyivTomorrowYyyymmdd(),
    pipelineDayOffset: pipelineDayOffset.value(),
  });
});

export const schedulePollStub = onSchedule(
  {
    schedule: "every 60 minutes",
    timeZone: "Europe/Kyiv",
  },
  async () => {
    try {
      const db = getFirestore();
      const messaging = getMessaging();
      const html = await fetchOblenergoStub(oblenergoSourceUrl.value());
      const pairs = parseScheduleHtmlStub(html);
      const day = effectivePipelineDayYyyymmdd();
      await applyScheduleUpdate(
        db,
        messaging,
        pipelineRegionId.value(),
        pipelineQueueId.value(),
        day,
        pairs,
      );
    } catch (e) {
      logger.error("schedulePollStub_failed", e);
      throw e;
    }
  },
);

/** Ручний запуск пайплайну: `?key=` має збігатися з `HTTP_SCHEDULE_KEY` (fn-cloud-scheduler / тести). */
export const runSchedulePipelineHttp = onRequest(async (req, res) => {
  const expected = httpScheduleKey.value();
  if (!expected || req.query.key !== expected) {
    res.status(403).send("forbidden");
    return;
  }
  try {
    const db = getFirestore();
    const messaging = getMessaging();
    const html = await fetchOblenergoStub(oblenergoSourceUrl.value());
    const pairs = parseScheduleHtmlStub(html);
    const result = await applyScheduleUpdate(
      db,
      messaging,
      pipelineRegionId.value(),
      pipelineQueueId.value(),
      effectivePipelineDayYyyymmdd(),
      pairs,
    );
    res.status(200).json({ ok: true, ...result });
  } catch (e) {
    logger.error("runSchedulePipelineHttp", e);
    res.status(500).json({ ok: false, error: String(e) });
  }
});
