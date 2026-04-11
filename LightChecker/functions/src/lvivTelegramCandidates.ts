const LVIV_CHANNEL = "lvivoblenergo";

/** Скільки останніх постів-кандидатів переглядаємо (від новішого до старішого). */
export const LVIV_MAX_SCHEDULE_CANDIDATES = 5;

export type LvivScheduleCandidate = {
  postId: string;
  imageUrl: string;
};

/**
 * Пости з коротким текстом і картинкою графіка.
 * У HTML t.me/s/ зазвичай від старого до нового; повертаємо від **новішого** до старішого.
 */
export function collectLvivScheduleCandidates(
  html: string,
  maxPosts = LVIV_MAX_SCHEDULE_CANDIDATES,
): LvivScheduleCandidate[] {
  const blocks = html.split("tgme_widget_message_wrap");
  const chronological: LvivScheduleCandidate[] = [];

  for (const block of blocks) {
    const postMatch = block.match(
      new RegExp(`data-post="${LVIV_CHANNEL}/(\\d+)"`),
    );
    if (!postMatch) continue;

    const textMatch = block.match(
      /tgme_widget_message_text[^>]*>([\s\S]*?)<\/div>/,
    );
    const text = textMatch
      ? textMatch[1].replace(/<[^>]+>/g, "").trim()
      : "";
    if (text.length > 10) continue;

    let lastImg: string | null = null;
    const imgRe = /https:\/\/cdn[^"'\s)]+\.(?:jpg|jpeg|png|webp)/gi;
    let im: RegExpExecArray | null;
    while ((im = imgRe.exec(block)) !== null) {
      if (!im[0].includes("emoji") && !im[0].includes("user_photo")) {
        lastImg = im[0];
      }
    }
    if (!lastImg) continue;

    chronological.push({ postId: postMatch[1], imageUrl: lastImg });
  }

  const newestFirst = chronological.slice().reverse();
  const seen = new Set<string>();
  const unique: LvivScheduleCandidate[] = [];
  for (const c of newestFirst) {
    if (seen.has(c.postId)) continue;
    seen.add(c.postId);
    unique.push(c);
    if (unique.length >= maxPosts) break;
  }
  return unique;
}

/** Digest верхніх кандидатів для populate_meta (той самий список postId → без OCR). */
export function lvivCandidatesFingerprint(
  html: string,
  maxPosts = LVIV_MAX_SCHEDULE_CANDIDATES,
): string | null {
  const candidates = collectLvivScheduleCandidates(html, maxPosts);
  if (candidates.length === 0) return null;
  const ids = candidates.map((c) => c.postId).join("|");
  return `${LVIV_CHANNEL}|${ids}`;
}
