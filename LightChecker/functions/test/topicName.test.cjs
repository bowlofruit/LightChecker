const test = require("node:test");
const assert = require("node:assert/strict");
const { fcmTopicForRegionQueue } = require("../lib/topicName.js");

test("matches Android FirebaseTopicNames pattern", () => {
  assert.equal(fcmTopicForRegionQueue("kyiv", "2.1"), "lc_kyiv_2.1");
});

test("sanitizes unsafe characters", () => {
  assert.equal(fcmTopicForRegionQueue("a/b", "q#1"), "lc_a_b_q_1");
});

test("truncates to 200 chars", () => {
  const long = "x".repeat(300);
  assert.equal(fcmTopicForRegionQueue(long, "q").length, 200);
});
