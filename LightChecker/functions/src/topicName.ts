/** Дзеркалить `FirebaseTopicNames.kt` (Android). */
const topicUnsafe = /[^a-zA-Z0-9-_.~%]/g;

export function fcmTopicForRegionQueue(regionId: string, queueId: string): string {
  const raw = `lc_${regionId}_${queueId}`;
  return raw.replace(topicUnsafe, "_").slice(0, 200);
}
