const test = require("node:test");
const assert = require("node:assert/strict");
const { parseScheduleHtmlStub } = require("../lib/parseScheduleHtmlStub.js");

test("parses en dash and hyphen ranges", () => {
  const html = "<p>08:00–10:00</p> bad 14:00-15:30";
  const pairs = parseScheduleHtmlStub(html);
  assert.deepEqual(pairs, [
    [480, 600],
    [840, 930],
  ]);
});

test("empty when no matches", () => {
  assert.deepEqual(parseScheduleHtmlStub("<html></html>"), []);
});
