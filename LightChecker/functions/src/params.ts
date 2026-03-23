import { defineString } from "firebase-functions/params";

/** URL HTML джерела; порожньо — використовується вбудована заглушка в `fetchOblenergoStub`. */
export const oblenergoSourceUrl = defineString("OBLENERGO_SOURCE_URL", {
  default: "",
});

export const pipelineRegionId = defineString("PIPELINE_REGION_ID", {
  default: "kyiv",
});

export const pipelineQueueId = defineString("PIPELINE_QUEUE_ID", {
  default: "1",
});

/** `0` — календарний «сьогодні» в Europe/Kyiv; `1` — «завтра» (поле `d` у Firestore). */
export const pipelineDayOffset = defineString("PIPELINE_DAY_OFFSET", {
  default: "0",
});

/**
 * Якщо задано, HTTPS `runSchedulePipelineHttp` приймає `?key=...` (інакше 403).
 * Залиште порожнім у prod, якщо HTTP-тригер не потрібен.
 */
export const httpScheduleKey = defineString("HTTP_SCHEDULE_KEY", {
  default: "",
});
