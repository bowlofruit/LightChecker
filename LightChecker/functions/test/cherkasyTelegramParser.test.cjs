const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const {
  CherkasyTelegramParser,
  parseAllQueues,
} = require("../lib/parsers/cherkasyTelegramParser");

describe("CherkasyTelegramParser", () => {
  it("parses per-subqueue lines (ГПВ format)", () => {
    const text = [
      "Години відсутності електропостачання:",
      "4.1 18:00 - 20:00",
      "4.2 18:00 - 20:00",
      "5.1 20:00 - 22:00",
      "5.2 20:00 - 22:00",
      "6.1 22:00 - 23:00",
    ].join("\n");

    const parser = new CherkasyTelegramParser("4.1");
    const result = parser.parse(text);
    assert.deepStrictEqual(result, [[1080, 1200]]); // 18*60=1080, 20*60=1200
  });

  it("parses multiple intervals for same queue", () => {
    const text = "1.1 07:00 - 09:00\n1.1 14:00 - 16:00";
    const parser = new CherkasyTelegramParser("1.1");
    const result = parser.parse(text);
    assert.deepStrictEqual(result, [
      [420, 540],
      [840, 960],
    ]);
  });

  it("returns empty when target queue not found", () => {
    const text = "4.1 18:00 - 20:00\n5.1 20:00 - 22:00";
    const parser = new CherkasyTelegramParser("9.9");
    assert.deepStrictEqual(parser.parse(text), []);
  });

  it("falls back to ГОП format when no queue lines found", () => {
    const text =
      "8 квітня з 07:00 до 10:00 та з 19:00 до 22:00 будуть застосовані ГОП";
    const parser = new CherkasyTelegramParser();
    const result = parser.parse(text);
    assert.deepStrictEqual(result, [
      [420, 600],
      [1140, 1320],
    ]);
  });

  it("returns empty for unrelated text", () => {
    const parser = new CherkasyTelegramParser();
    assert.deepStrictEqual(parser.parse("Шановні споживачі!"), []);
  });
});

describe("parseAllQueues", () => {
  it("returns map of all queues from a post", () => {
    const text = [
      "4.1 18:00 - 20:00",
      "4.2 18:00 - 20:00",
      "5.1 20:00 - 22:00",
    ].join("\n");

    const result = parseAllQueues(text);
    assert.strictEqual(result.size, 3);
    assert.deepStrictEqual(result.get("4.1"), [[1080, 1200]]);
    assert.deepStrictEqual(result.get("4.2"), [[1080, 1200]]);
    assert.deepStrictEqual(result.get("5.1"), [[1200, 1320]]);
  });

  it("returns empty map for no matches", () => {
    assert.strictEqual(parseAllQueues("no data").size, 0);
  });
});
