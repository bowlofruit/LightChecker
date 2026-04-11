const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const {
  collectLvivScheduleCandidates,
  lvivCandidatesFingerprint,
  LVIV_MAX_SCHEDULE_CANDIDATES,
} = require("../lib/lvivTelegramCandidates");

function block(postId, textLen, imgSuffix) {
  const text = textLen > 10 ? "x".repeat(textLen) : "";
  return (
    ` data-post="lvivoblenergo/${postId}"` +
    `<div class="tgme_widget_message_text">${text}</div>` +
    `<img src="https://cdn4.telegram-cdn.com/file/${imgSuffix}.jpg" />`
  );
}

function page(...chunks) {
  return chunks.join("tgme_widget_message_wrap");
}

describe("collectLvivScheduleCandidates", () => {
  it("returns newest first (reverse page order)", () => {
    const html = page(block("1", 0, "a"), block("2", 0, "b"), block("3", 0, "c"));
    const c = collectLvivScheduleCandidates(html);
    assert.deepEqual(
      c.map((x) => x.postId),
      ["3", "2", "1"],
    );
  });

  it("caps at LVIV_MAX_SCHEDULE_CANDIDATES", () => {
    const parts = [];
    for (let i = 1; i <= 8; i++) {
      parts.push(block(String(i), 0, `i${i}`));
    }
    const html = page(...parts);
    const c = collectLvivScheduleCandidates(html);
    assert.equal(c.length, LVIV_MAX_SCHEDULE_CANDIDATES);
    assert.equal(c[0].postId, "8");
    assert.equal(c[4].postId, "4");
  });

  it("skips long text blocks", () => {
    const html = page(block("1", 20, "a"), block("2", 0, "b"));
    const c = collectLvivScheduleCandidates(html);
    assert.deepEqual(
      c.map((x) => x.postId),
      ["2"],
    );
  });
});

describe("lvivCandidatesFingerprint", () => {
  it("joins post ids", () => {
    const html = page(block("10", 0, "a"), block("20", 0, "b"));
    assert.equal(lvivCandidatesFingerprint(html), "lvivoblenergo|20|10");
  });
});
