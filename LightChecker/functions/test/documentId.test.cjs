const test = require("node:test");
const assert = require("node:assert/strict");
const { firestoreDocumentId, sanitizeSegment } = require("../lib/documentId.js");

test("firestoreDocumentId matches Android ScheduleDocumentIds (dot sanitized)", () => {
  assert.equal(firestoreDocumentId("kyiv", "2.1"), "kyiv__2_1");
});

test("sanitizeSegment replaces unsafe chars", () => {
  assert.equal(sanitizeSegment("a/b"), "a_b");
});
