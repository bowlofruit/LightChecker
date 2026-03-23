import type { Firestore } from "firebase-admin/firestore";
import type { Messaging } from "firebase-admin/messaging";
import * as logger from "firebase-functions/logger";
import { firestoreDocumentId } from "./documentId";
import { fcmTopicForRegionQueue } from "./topicName";
import { normalizeIntervalPairs, pairsToFlatMinutes } from "./normalizeIntervals";
import { validateSchedulePayload } from "./validateSlots";

export type ApplyScheduleResult = { skipped: boolean; docId: string };

/**
 * Читає попередній документ, порівнює `s` для того ж `d`, підвищує `v` лише при зміні,
 * пише Firestore і шле data-only FCM (fn-load-previous-doc, fn-diff-decide-v, fn-write-schedule-doc, fn-fcm-*).
 */
export async function applyScheduleUpdate(
  db: Firestore,
  messaging: Messaging,
  regionId: string,
  queueId: string,
  dayYyyymmdd: number,
  rawPairs: [number, number][],
): Promise<ApplyScheduleResult> {
  const docId = firestoreDocumentId(regionId, queueId);
  const ref = db.collection("schedules").doc(docId);
  const snap = await ref.get();

  const merged = normalizeIntervalPairs(rawPairs);
  const flat = pairsToFlatMinutes(merged);

  let nextV = 1;
  if (snap.exists) {
    const p = snap.data()!;
    const prevD = Number(p.d);
    const prevS = JSON.stringify(p.s ?? []);
    const nextS = JSON.stringify(flat);
    if (prevD === dayYyyymmdd && prevS === nextS) {
      logger.info("schedule_unchanged_skip_fcm", { docId, d: dayYyyymmdd });
      return { skipped: true, docId };
    }
    nextV = prevD === dayYyyymmdd ? Number(p.v ?? 0) + 1 : 1;
  }

  const validated = validateSchedulePayload(1, nextV, dayYyyymmdd, flat);
  if (!validated.ok) {
    logger.error("schedule_validate_failed", { docId, reason: validated.reason });
    throw new Error(validated.reason);
  }

  const g = Math.floor(Date.now() / 1000);
  await ref.set({ f: 1, v: nextV, d: dayYyyymmdd, s: flat, g });

  try {
    await messaging.send({
      topic: fcmTopicForRegionQueue(regionId, queueId),
      data: {
        r: regionId,
        q: queueId,
        v: String(nextV),
        d: String(dayYyyymmdd),
      },
    });
    logger.info("schedule_applied_fcm_sent", { docId, v: nextV, d: dayYyyymmdd });
  } catch (e) {
    logger.warn("schedule_fcm_send_failed", { docId, err: String(e) });
  }

  return { skipped: false, docId };
}
