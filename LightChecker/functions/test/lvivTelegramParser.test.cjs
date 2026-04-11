const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const { parseLvivScheduleText } = require("../lib/parsers/lvivTelegramParser");

describe("parseLvivScheduleText", () => {
  it("puts single outage on combined status line into column 1.2, not 1.1", () => {
    const text = [
      "1.1 1.2",
      "Електроенергія є          Електроенергії немає з 06:00 по 08:00",
    ].join("\n");
    const m = parseLvivScheduleText(text);
    assert.deepEqual(m.get("1.1") ?? [], []);
    assert.deepEqual(m.get("1.2") ?? [], [[360, 480]]);
  });

  it("puts outage on separate right-column status line into 1.2", () => {
    const text = [
      "1.1 1.2",
      "Електроенергія є",
      "Електроенергії немає з 06:00 по 08:00",
    ].join("\n");
    const m = parseLvivScheduleText(text);
    assert.deepEqual(m.get("1.1") ?? [], []);
    assert.deepEqual(m.get("1.2") ?? [], [[360, 480]]);
  });

  it("splits two time ranges on one line between 1.1 and 1.2", () => {
    const text = ["1.1 1.2", "з 11:30 по 14:00      з 20:00 по 22:00"].join(
      "\n",
    );
    const m = parseLvivScheduleText(text);
    assert.deepEqual(m.get("1.1") ?? [], [[690, 840]]);
    assert.deepEqual(m.get("1.2") ?? [], [[1200, 1320]]);
  });
});
