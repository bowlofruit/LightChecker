const test = require("node:test");
const assert = require("node:assert/strict");
const {
  normalizeIntervalPairs,
  pairsToFlatMinutes,
} = require("../lib/normalizeIntervals.js");

test("merges overlapping and touching", () => {
  const merged = normalizeIntervalPairs([
    [100, 200],
    [150, 250],
    [250, 300],
  ]);
  assert.deepEqual(merged, [[100, 300]]);
});

test("flat roundtrip", () => {
  const flat = pairsToFlatMinutes([
    [0, 60],
    [120, 180],
  ]);
  assert.deepEqual(flat, [0, 60, 120, 180]);
});
