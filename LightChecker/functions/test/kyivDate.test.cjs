const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const {
  kyivTodayYyyymmdd,
  yyyymmddFromGraphicDayMonth,
} = require("../lib/kyivDate");
const {
  extractLvivScheduleDayYyyymmdd,
} = require("../lib/lvivGraphicDate");

describe("yyyymmddFromGraphicDayMonth", () => {
  it("uses same year when graphic is yesterday (10.04 vs 11.04)", () => {
    const now = new Date(Date.UTC(2026, 3, 11, 10, 0, 0));
    assert.equal(kyivTodayYyyymmdd(now), 20260411);
    assert.equal(yyyymmddFromGraphicDayMonth(10, 4, now), 20260410);
  });

  it("rolls year forward when Dec 31 and graphic shows 01.01", () => {
    const now = new Date(Date.UTC(2025, 11, 31, 12, 0, 0));
    assert.equal(kyivTodayYyyymmdd(now), 20251231);
    assert.equal(yyyymmddFromGraphicDayMonth(1, 1, now), 20260101);
  });
});

describe("extractLvivScheduleDayYyyymmdd", () => {
  it("parses DD.MM from OCR snippet", () => {
    const now = new Date(Date.UTC(2026, 3, 11, 10, 0, 0));
    const text = "Графік 10.04.2026\n1.1 1.2";
    assert.equal(extractLvivScheduleDayYyyymmdd(text, now), 20260410);
  });

  it("skips queue-like 6.2", () => {
    const now = new Date(Date.UTC(2026, 3, 11, 10, 0, 0));
    const text = "6.2 черга\nз 10:00 по 12:00";
    assert.equal(extractLvivScheduleDayYyyymmdd(text, now), null);
  });
});
