/**
 * Live test: scan DTEK Telegram channel → OCR all schedule images → group by city.
 * Run: node functions/test/live-dtek-test.cjs [kyiv|odesa|dnipro]
 */
const { DtekTelegramParser } = require("../lib/parsers/dtekTelegramParser");

const targetRegion = process.argv[2] || "all";

function formatMin(min) {
  return String(Math.floor(min / 60)).padStart(2, "0") + ":" + String(min % 60).padStart(2, "0");
}

async function parseCity(regionId, label) {
  console.log(`\n=== ${label} (${regionId}) ===`);
  const parser = new DtekTelegramParser(regionId);
  const { scheduleDayYyyymmdd, queues } = await parser.parseChannelAsync();
  if (scheduleDayYyyymmdd != null) {
    console.log(`  scheduleDayYyyymmdd: ${scheduleDayYyyymmdd}`);
  }
  if (queues.size === 0) {
    console.log("  (no schedule data found)");
    return;
  }
  // Sort by queue id
  const sorted = [...queues.entries()].sort((a, b) => a[0].localeCompare(b[0], undefined, { numeric: true }));
  for (const [queueId, intervals] of sorted) {
    const formatted = intervals.map(([s, e]) => `${formatMin(s)}-${formatMin(e)}`).join(", ");
    console.log(`  ${queueId}: ${formatted}`);
  }
  console.log(`  Total: ${queues.size} queues`);
}

(async () => {
  const start = Date.now();

  if (targetRegion === "all" || targetRegion === "kyiv") {
    await parseCity("kyiv", "Київ");
  }
  if (targetRegion === "all" || targetRegion === "odesa") {
    await parseCity("odesa", "Одеса");
  }
  if (targetRegion === "all" || targetRegion === "dnipro") {
    await parseCity("dnipro", "Дніпро");
  }

  console.log(`\nDone in ${((Date.now() - start) / 1000).toFixed(1)}s`);
})().catch(console.error);
