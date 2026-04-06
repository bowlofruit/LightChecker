const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const { RegexTimeParser } = require("../lib/parsers/regexTimeParser");
const { getParser } = require("../lib/parsers/index");

describe("RegexTimeParser", () => {
  const parser = new RegexTimeParser();

  it("parses en-dash time ranges", () => {
    const result = parser.parse("08:00\u201310:00");
    assert.deepEqual(result, [[480, 600]]);
  });

  it("parses hyphen time ranges", () => {
    const result = parser.parse("14:00-15:30");
    assert.deepEqual(result, [[840, 930]]);
  });

  it("returns empty for no matches", () => {
    const result = parser.parse("no time ranges here");
    assert.deepEqual(result, []);
  });
});

describe("getParser", () => {
  it("returns regex parser", () => {
    const parser = getParser("regex");
    assert.ok(parser);
    assert.equal(typeof parser.parse, "function");
    assert.ok(parser instanceof RegexTimeParser);
  });

  it("returns dtek_html parser", () => {
    const parser = getParser("dtek_html");
    assert.ok(parser);
    assert.equal(typeof parser.parse, "function");
  });

  it("returns image_ocr parser", () => {
    const parser = getParser("image_ocr");
    assert.ok(parser);
    assert.equal(typeof parser.parse, "function");
  });

  it("throws for unknown parser name", () => {
    assert.throws(() => getParser("nonexistent"), {
      message: "Unknown parser: nonexistent",
    });
  });
});
