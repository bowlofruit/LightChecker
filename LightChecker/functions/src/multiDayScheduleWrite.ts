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

/** `fv` = остання версія, для якої FCM було успішно надіслано (0 — ще не надсилали). */
export type DayEntry = { v: number; s: number[]; g: number; fv: number };

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
        fv: Number(o.fv ?? 0),
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
    fv: Number(data.fv ?? 0),
  };
  return days;
}

function pruneToWindow(
  days: Record<string, DayEntry>,
  allowed: Set<string>,
): Record<string, DayEntry> {
  const pruned: Record<string, DayEntry> = {};
  for (const k of allowed) {
    if (days[k]) pruned[k] = days[k]!;
  }
  return pruned;
}

/** Результат транзакційної частини: пропустити, переслати FCM (без bump), або новий запис. */
type TxDecision =
  | { kind: "skip" }
  | { kind: "send"; version: number };

/**
 * Оновлює один календарний день у документі `schedules/{region}__{queue}`.
 * Тримає лише сьогодні та завтра (Europe/Kyiv); інші ключі з `days` відкидаються.
 *
 * Запис read-modify-write виконується в транзакції (гонка версій між паралельними
 * запусками cron/HTTP), а FCM надсилається після коміту. Якщо FCM падає, `fv`
 * лишається позаду `v`, тож наступний запуск повторно надішле сповіщення.
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

  const decision: TxDecision = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const days = snap.exists
      ? loadDaysInWindow(snap.data() as Record<string, unknown>, allowed)
      : {};

    const prevEntry = days[dayKey];
    const contentSame =
      !!prevEntry && JSON.stringify(prevEntry.s) === JSON.stringify(flat);

    if (contentSame) {
      if (prevEntry!.fv >= prevEntry!.v) {
        return { kind: "skip" };
      }
      return { kind: "send", version: prevEntry!.v };
    }

    const nextV = prevEntry ? prevEntry.v + 1 : 1;
    const validated = validateSchedulePayload(1, nextV, dayYyyymmdd, flat);
    if (!validated.ok) {
      throw new Error(validated.reason);
    }

    days[dayKey] = {
      v: nextV,
      s: flat,
      g: Math.floor(Date.now() / 1000),
      fv: prevEntry?.fv ?? 0,
    };
    tx.set(ref, { f: 2, days: pruneToWindow(days, allowed) });
    return { kind: "send", version: nextV };
  });

  if (decision.kind === "skip") {
    logger.info("schedule_unchanged_skip_fcm", { docId, d: dayYyyymmdd });
    return { skipped: true, docId };
  }

  const version = decision.version;
  try {
    await messaging.send({
      topic: fcmTopicForRegionQueue(regionId, queueId),
      data: { r: regionId, q: queueId, v: String(version), d: dayKey },
    });
  } catch (e) {
    logger.warn("schedule_fcm_send_failed", { docId, v: version, err: String(e) });
    return { skipped: false, docId, version };
  }

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    if (!snap.exists) return;
    const days = loadDaysInWindow(snap.data() as Record<string, unknown>, allowed);
    const entry = days[dayKey];
    if (!entry || entry.fv >= version) return;
    entry.fv = version;
    tx.set(ref, { f: 2, days: pruneToWindow(days, allowed) });
  });

  logger.info("schedule_applied_fcm_sent", { docId, v: version, d: dayYyyymmdd });
  return { skipped: false, docId, version };
}
