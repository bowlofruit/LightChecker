import type { Firestore } from "firebase-admin/firestore";
import type { Messaging } from "firebase-admin/messaging";
import * as logger from "firebase-functions/logger";
import { firestoreDocumentId } from "./documentId";
import { kyivTodayYyyymmdd, kyivTomorrowYyyymmdd } from "./kyivDate";
import { fcmTopicForRegionQueue } from "./topicName";
import { validateSchedulePayload } from "./validateSlots";

export type ApplyScheduleResult = {
  skipped: boolean;
  docId: string;
  version?: number;
};

export type DayEntry = { v: number; s: number[]; g: number };

function allowedDayKeys(now: Date): Set<string> {
  return new Set([
    String(kyivTodayYyyymmdd(now)),
    String(kyivTomorrowYyyymmdd(now)),
  ]);
}

/** Зліпити days лише для вікна [сьогодні, завтра] Kyiv з існуючого документа (f=2 або legacy f=1). */
export function loadDaysInWindow(
  data: Record<string, unknown> | undefined,
  allowed: Set<string>,
): Record<string, DayEntry> {
  const days: Record<string, DayEntry> = {};
  if (!data) return days;

  const f = Number(data.f ?? 1);
  if (f === 2 && data.days && typeof data.days === "object" && data.days !== null) {
    const raw = data.days as Record<string, unknown>;
    for (const k of Object.keys(raw)) {
      if (!allowed.has(k)) continue;
      const val = raw[k];
      if (!val || typeof val !== "object") continue;
      const o = val as Record<string, unknown>;
      if (!Array.isArray(o.s)) continue;
      days[k] = {
        v: Number(o.v ?? 1),
        s: o.s.map((x) => Number(x)),
        g: Number(o.g ?? 0),
      };
    }
    return days;
  }

  if (data.d == null || !Array.isArray(data.s)) return days;
  const dk = String(Number(data.d));
  if (!allowed.has(dk)) return days;
  days[dk] = {
    v: Number(data.v ?? 1),
    s: (data.s as unknown[]).map((x) => Number(x)),
    g: Number(data.g ?? 0),
  };
  return days;
}

/**
 * Оновлює один календарний день у документі `schedules/{region}__{queue}`.
 * Тримає лише сьогодні та завтра (Europe/Kyiv); інші ключі з `days` відкидаються.
 */
export async function applyScheduleDayWrite(
  db: Firestore,
  messaging: Messaging,
  regionId: string,
  queueId: string,
  dayYyyymmdd: number,
  flat: number[],
  now: Date = new Date(),
): Promise<ApplyScheduleResult> {
  const docId = firestoreDocumentId(regionId, queueId);
  const ref = db.collection("schedules").doc(docId);
  const snap = await ref.get();
  const allowed = allowedDayKeys(now);
  const dayKey = String(dayYyyymmdd);

  if (!allowed.has(dayKey)) {
    logger.warn("schedule_day_outside_today_tomorrow", {
      docId,
      dayYyyymmdd,
      allowed: [...allowed],
    });
    return { skipped: true, docId };
  }

  let days: Record<string, DayEntry> = {};
  if (snap.exists) {
    const p = snap.data() as Record<string, unknown>;
    days = loadDaysInWindow(p, allowed);
  }

  const prevEntry = days[dayKey];
  const prevFlat = JSON.stringify(prevEntry?.s ?? []);
  const nextFlat = JSON.stringify(flat);
  if (prevEntry && prevFlat === nextFlat) {
    logger.info("schedule_unchanged_skip_fcm", { docId, d: dayYyyymmdd });
    return { skipped: true, docId };
  }

  const nextV = prevEntry ? prevEntry.v + 1 : 1;
  const validated = validateSchedulePayload(1, nextV, dayYyyymmdd, flat);
  if (!validated.ok) {
    logger.error("schedule_validate_failed", {
      docId,
      reason: validated.reason,
    });
    throw new Error(validated.reason);
  }

  days[dayKey] = {
    v: nextV,
    s: flat,
    g: Math.floor(Date.now() / 1000),
  };

  const pruned: Record<string, DayEntry> = {};
  for (const k of allowed) {
    if (days[k]) pruned[k] = days[k]!;
  }

  await ref.set({ f: 2, days: pruned });

  try {
    await messaging.send({
      topic: fcmTopicForRegionQueue(regionId, queueId),
      data: {
        r: regionId,
        q: queueId,
        v: String(nextV),
        d: dayKey,
      },
    });
    logger.info("schedule_applied_fcm_sent", {
      docId,
      v: nextV,
      d: dayYyyymmdd,
    });
  } catch (e) {
    logger.warn("schedule_fcm_send_failed", { docId, err: String(e) });
  }

  return { skipped: false, docId, version: nextV };
}
