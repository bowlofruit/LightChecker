/**
 * Cheap "same post as before?" check using Telegram public preview HTML.
 * Uses data-post="channel/postId" — stable per message; avoids OCR when unchanged.
 */

const CHERKASY_CHANNEL = "pat_cherkasyoblenergo";

function stripTgMessageText(htmlFragment: string): string {
  return htmlFragment
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&amp;/g, "&")
    .trim();
}

/** Latest schedule-style message fingerprint, or null if none matched. */
export function cherkasySchedulePostFingerprint(html: string): string | null {
  const blocks = html.split("tgme_widget_message_wrap");
  let last: string | null = null;
  for (const block of blocks) {
    const pm = block.match(
      new RegExp(`data-post="${CHERKASY_CHANNEL}/(\\d+)"`),
    );
    if (!pm) continue;
    const textMatch = block.match(
      /tgme_widget_message_text[^>]*>([\s\S]*?)<\/div>/,
    );
    const text = textMatch ? stripTgMessageText(textMatch[1]) : "";
    if (/\d+\.\d+\s+\d{1,2}:\d{2}/.test(text)) {
      last = `${CHERKASY_CHANNEL}/${pm[1]}`;
    }
  }
  return last;
}
