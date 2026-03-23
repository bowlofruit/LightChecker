const test = require("node:test");
const assert = require("node:assert/strict");
const { validateSchedulePayload } = require("../lib/validateSlots.js");

test("rejects odd s length", () => {
  const r = validateSchedulePayload(1, 1, 20260323, [0, 60, 120]);
  assert.equal(r.ok, false);
});

test("accepts empty s", () => {
  const r = validateSchedulePayload(1, 1, 20260323, []);
  assert.equal(r.ok, true);
});

test("rejects f != 1", () => {
  const r = validateSchedulePayload(999, 1, 20260323, [0, 1]);
  assert.equal(r.ok, false);
});
