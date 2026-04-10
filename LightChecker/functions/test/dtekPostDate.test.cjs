const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const {
  extractDtekScheduleDayYyyymmdd,
  sliceDtekTextForRegion,
} = require("../lib/dtekPostDate");

describe("sliceDtekTextForRegion", () => {
  it("cuts between regional blocks", () => {
    const text =
      "Одещина. Застосовано графіки відключень на 9 квітня.\n" +
      "Київщина. Застосовано графіки відключень на 10 квітня.\n" +
      "Дніпропетровщина. На 11 квітня.";
    const odesaSlice = sliceDtekTextForRegion(text, "odesa");
    assert.match(odesaSlice, /9 квітня/);
    assert.doesNotMatch(odesaSlice, /10 квітня/);
    const kyivSlice = sliceDtekTextForRegion(text, "kyiv");
    assert.match(kyivSlice, /10 квітня/);
    assert.doesNotMatch(kyivSlice, /9 квітня/);
  });
});

describe("extractDtekScheduleDayYyyymmdd", () => {
  const now = new Date(Date.UTC(2026, 3, 11, 12, 0, 0));

  it("reads date from regional slice when multiple regions in one post", () => {
    const text =
      "Одещина. Застосовано графіки відключень на 9 квітня.\n" +
      "Київщина. Застосовано графіки відключень на 10 квітня.";
    assert.equal(extractDtekScheduleDayYyyymmdd(text, "odesa", now), 20260409);
    assert.equal(extractDtekScheduleDayYyyymmdd(text, "kyiv", now), 20260410);
  });

  it("uses explicit year when present", () => {
    const text = "Київщина. На 5 березня 2025 року.";
    assert.equal(extractDtekScheduleDayYyyymmdd(text, "kyiv", now), 20250305);
  });
});
