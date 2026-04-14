const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const {
  extractCherkasyScheduleDayYyyymmdd,
  pickCherkasyScheduleMessage,
} = require("../lib/cherkasyPostDate");

/** Фіксований «зараз» у зоні Kyiv (11 квітня 2026). */
const KYIV_2026_04_11 = new Date("2026-04-11T15:00:00+03:00");

describe("extractCherkasyScheduleDayYyyymmdd", () => {
  it("reads day and month from post body", () => {
    const text =
      "Оновлений графік погодинних вимкнень на 10 квітня за командою НЕК";
    assert.equal(
      extractCherkasyScheduleDayYyyymmdd(text, KYIV_2026_04_11),
      20260410,
    );
  });

  it("reads explicit year", () => {
    const text = "графік на 5 травня 2027 року";
    assert.equal(
      extractCherkasyScheduleDayYyyymmdd(text, KYIV_2026_04_11),
      20270505,
    );
  });
});

describe("pickCherkasyScheduleMessage", () => {
  it("prefers post dated today over newer post for yesterday", () => {
    const msgApr11 = [
      "Оновлений графік на 11 квітня.",
      "",
      "Години відсутності:",
      "4.2 06:00 - 08:00",
    ].join("\n");
    const msgApr10 = [
      "Оновлений графік на 10 квітня.",
      "",
      "Години відсутності:",
      "4.1 22:00 - 24:00",
      "6.1 23:00 - 24:00",
    ].join("\n");
    const messages = [msgApr11, msgApr10];
    const picked = pickCherkasyScheduleMessage(messages, KYIV_2026_04_11);
    assert.equal(picked.dayYyyymmdd, 20260411);
    assert.match(picked.text, /4\.2/);
    assert.doesNotMatch(picked.text, /4\.1 22:00/);
  });

  it("when only yesterday in feed, returns that post and date", () => {
    const msgApr10 = "на 10 квітня\n\n4.1 22:00 - 24:00";
    const picked = pickCherkasyScheduleMessage([msgApr10], KYIV_2026_04_11);
    assert.equal(picked.dayYyyymmdd, 20260410);
  });
});
