import type { Firestore } from "firebase-admin/firestore";
import type { Messaging } from "firebase-admin/messaging";
import { applyScheduleDayWrite } from "./multiDayScheduleWrite";
import { normalizeIntervalPairs, pairsToFlatMinutes } from "./normalizeIntervals";

export type { ApplyScheduleResult } from "./multiDayScheduleWrite";

/**
 * Пише один день у `schedules/{region}__{queue}` (f=2, days: сьогодні|завтра за Kyiv).
 */
export async function applyScheduleUpdate(
  db: Firestore,
  messaging: Messaging,
  regionId: string,
  queueId: string,
  dayYyyymmdd: number,
  rawPairs: [number, number][],
  now: Date = new Date(),
) {
  const merged = normalizeIntervalPairs(rawPairs);
  const flat = pairsToFlatMinutes(merged);
  return applyScheduleDayWrite(
    db,
    messaging,
    regionId,
    queueId,
    dayYyyymmdd,
    flat,
    now,
  );
}
