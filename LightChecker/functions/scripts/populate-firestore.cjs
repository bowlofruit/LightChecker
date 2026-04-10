/**
 * Populate Firestore with real schedule data from Telegram channels.
 *
 * Works in two modes:
 *   1. GitHub Actions: uses GOOGLE_APPLICATION_CREDENTIALS env var (Service Account)
 *   2. Local: uses Firebase CLI credentials or Application Default Credentials
 *
 * Usage:
 *   node functions/scripts/populate-firestore.cjs
 */

const admin = require("firebase-admin");
const { fcmTopicForRegionQueue } = require("../lib/topicName");
const { parseAllQueues } = require("../lib/parsers/cherkasyTelegramParser");
const { DtekTelegramParser } = require("../lib/parsers/dtekTelegramParser");
const { LvivTelegramParser } = require("../lib/parsers/lvivTelegramParser");
const { firestoreDocumentId } = require("../lib/documentId");
const { normalizeIntervalPairs, pairsToFlatMinutes } = require("../lib/normalizeIntervals");
const { kyivTodayYyyymmdd } = require("../lib/kyivDate");

// Initialize Firebase Admin — auto-detects credentials from env
if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  admin.initializeApp();
} else {
  admin.initializeApp({ projectId: "lightchecker-ebe94" });
}
const db = admin.firestore();

function formatMin(min) {
  return String(Math.floor(min / 60)).padStart(2, "0") + ":" + String(min % 60).padStart(2, "0");
}

async function writeSchedule(regionId, queueId, day, intervals) {
  const docId = firestoreDocumentId(regionId, queueId);
  const normalized = normalizeIntervalPairs(intervals);
  const flat = pairsToFlatMinutes(normalized);

  const docRef = db.collection("schedules").doc(docId);
  const prev = await docRef.get();
  const prevData = prev.exists ? prev.data() : null;

  // Version management
  let version = 1;
  if (prevData) {
    const prevFlat = JSON.stringify(prevData.s || []);
    const nextFlat = JSON.stringify(flat);
    if (prevData.d === day && prevFlat === nextFlat) {
      return { skipped: true, docId };
    }
    if (prevData.d === day) {
      version = (prevData.v || 0) + 1;
    }
  }

  await docRef.set({
    f: 1,
    v: version,
    d: day,
    s: flat,
    g: Date.now(),
  });

  // Send FCM push to trigger background sync on devices
  const topic = fcmTopicForRegionQueue(regionId, queueId);
  try {
    await admin.messaging().send({
      topic,
      data: { r: regionId, q: queueId, v: String(version), d: String(day) },
    });
  } catch (e) {
    // FCM may fail if no subscribers — not critical
  }

  return { skipped: false, docId, version, slots: flat.length / 2 };
}

async function processCherkasy(day) {
  console.log("\n=== Черкаси ===");
  try {
    const res = await fetch("https://t.me/s/pat_cherkasyoblenergo", {
      headers: { "User-Agent": "LightChecker-Populate/1.0" },
      signal: AbortSignal.timeout(15000),
    });
    const html = await res.text();

    const msgRe = /<div class="tgme_widget_message_text[^"]*"[^>]*>([\s\S]*?)<\/div>/g;
    const messages = [];
    let m;
    while ((m = msgRe.exec(html)) !== null) {
      messages.push(
        m[1].replace(/<br\s*\/?>/gi, "\n").replace(/<[^>]+>/g, "").replace(/&amp;/g, "&").trim()
      );
    }

    const scheduleMsg = messages.filter((msg) => /\d+\.\d+\s+\d{1,2}:\d{2}/.test(msg)).pop();
    if (!scheduleMsg) { console.log("  No schedule found"); return; }

    const allQueues = parseAllQueues(scheduleMsg);
    for (const [queueId, intervals] of allQueues) {
      const result = await writeSchedule("cherkasy", queueId, day, intervals);
      const fmt = intervals.map(([s, e]) => `${formatMin(s)}-${formatMin(e)}`).join(", ");
      console.log(`  ${queueId}: ${fmt} → ${result.skipped ? "SKIP" : "v" + result.version}`);
    }
  } catch (e) {
    console.error("  Error:", e.message);
  }
}

async function processDtek(regionId, label, day) {
  console.log(`\n=== ${label} ===`);
  try {
    const parser = new DtekTelegramParser(regionId);
    const queues = await parser.parseChannelAsync();
    if (queues.size === 0) { console.log("  No data found"); return; }

    for (const [queueId, intervals] of queues) {
      const result = await writeSchedule(regionId, queueId, day, intervals);
      const fmt = intervals.map(([s, e]) => `${formatMin(s)}-${formatMin(e)}`).join(", ");
      console.log(`  ${queueId}: ${fmt} → ${result.skipped ? "SKIP" : "v" + result.version}`);
    }
  } catch (e) {
    console.error("  Error:", e.message);
  }
}

async function processLviv(day) {
  console.log("\n=== Львів ===");
  try {
    const parser = new LvivTelegramParser();
    const queues = await parser.parseChannelAsync();
    if (queues.size === 0) { console.log("  No data found"); return; }

    for (const [queueId, intervals] of queues) {
      const result = await writeSchedule("lviv", queueId, day, intervals);
      const fmt = intervals.map(([s, e]) => `${formatMin(s)}-${formatMin(e)}`).join(", ");
      console.log(`  ${queueId}: ${fmt} → ${result.skipped ? "SKIP" : "v" + result.version}`);
    }
  } catch (e) {
    console.error("  Error:", e.message);
  }
}

async function main() {
  const day = kyivTodayYyyymmdd();
  console.log("Date:", day, "(Kyiv time)");
  console.log("Project: lightchecker-ebe94");

  await processCherkasy(day);
  await processDtek("kyiv", "Київ", day);
  await processDtek("odesa", "Одеса", day);
  await processDtek("dnipro", "Дніпро", day);
  await processLviv(day);

  console.log("\nDone! Check Firestore: https://console.firebase.google.com/project/lightchecker-ebe94/firestore");
  process.exit(0);
}

main().catch((e) => {
  console.error("Fatal:", e);
  process.exit(1);
});
