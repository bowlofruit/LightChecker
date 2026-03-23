const unsafeSegment = /[^a-zA-Z0-9_-]/g;

export function sanitizeSegment(segment: string): string {
  return segment.replace(unsafeSegment, "_");
}

export function firestoreDocumentId(regionId: string, queueId: string): string {
  return `${sanitizeSegment(regionId)}__${sanitizeSegment(queueId)}`;
}
