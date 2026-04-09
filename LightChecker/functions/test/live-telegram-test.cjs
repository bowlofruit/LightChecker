/**
 * Live test: fetch Cherkasy oblenergo Telegram → parse → print results.
 * Run: node functions/test/live-telegram-test.cjs
 */

const { CherkasyTelegramParser, parseAllQueues } = require("../lib/parsers/cherkasyTelegramParser");

async function main() {
  const url = "https://t.me/s/pat_cherkasyoblenergo";
  console.log(`Fetching ${url} ...`);

  const res = await fetch(url, {
    headers: { "User-Agent": "LightChecker-Test/1.0" },
    signal: AbortSignal.timeout(15000),
  });

  if (!res.ok) {
    console.error(`HTTP ${res.status}`);
    process.exit(1);
  }

  const html = await res.text();
  console.log(`Fetched ${html.length} bytes\n`);

  // Extract text from Telegram HTML (message bodies)
  const msgRegex = /<div class="tgme_widget_message_text[^"]*"[^>]*>([\s\S]*?)<\/div>/g;
  const messages = [];
  let m;
  while ((m = msgRegex.exec(html)) !== null) {
    // Strip HTML tags, decode entities
    const text = m[1]
      .replace(/<br\s*\/?>/gi, "\n")
      .replace(/<[^>]+>/g, "")
      .replace(/&amp;/g, "&")
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">")
      .replace(/&quot;/g, '"')
      .replace(/&#(\d+);/g, (_, c) => String.fromCharCode(c));
    messages.push(text.trim());
  }

  console.log(`Found ${messages.length} messages\n`);

  // Find schedule messages (contain queue patterns like "1.1 08:00")
  const scheduleMessages = messages.filter(
    (msg) => /\d+\.\d+\s+\d{1,2}:\d{2}/.test(msg) || /з\s+\d{1,2}:\d{2}\s+до/.test(msg)
  );

  console.log(`Schedule messages: ${scheduleMessages.length}\n`);

  if (scheduleMessages.length === 0) {
    console.log("No schedule data found. Last 3 messages:");
    messages.slice(-3).forEach((msg, i) => {
      console.log(`\n--- Message ${i + 1} ---`);
      console.log(msg.substring(0, 300));
    });
    return;
  }

  // Parse the most recent schedule message
  const latest = scheduleMessages[scheduleMessages.length - 1];
  console.log("=== Latest schedule message ===");
  console.log(latest.substring(0, 500));
  console.log();

  // Parse all queues
  const allQueues = parseAllQueues(latest);
  console.log(`Parsed ${allQueues.size} queues:`);
  for (const [queueId, intervals] of allQueues) {
    const formatted = intervals
      .map(([s, e]) => {
        const sh = String(Math.floor(s / 60)).padStart(2, "0");
        const sm = String(s % 60).padStart(2, "0");
        const eh = String(Math.floor(e / 60)).padStart(2, "0");
        const em = String(e % 60).padStart(2, "0");
        return `${sh}:${sm}-${eh}:${em}`;
      })
      .join(", ");
    console.log(`  ${queueId}: ${formatted}`);
  }

  // Also test single-queue parser
  console.log("\n=== Single queue parser (4.1) ===");
  const parser = new CherkasyTelegramParser("4.1");
  const result = parser.parse(latest);
  console.log(`  4.1: ${JSON.stringify(result)}`);
}

main().catch(console.error);
