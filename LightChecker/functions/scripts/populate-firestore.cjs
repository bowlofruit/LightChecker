/**
 * Populate Firestore with real schedule data from Telegram channels.
 *
 * Works in two modes:
 *   1. GitHub Actions: uses GOOGLE_APPLICATION_CREDENTIALS env var (Service Account)
 *   2. Local: uses Firebase CLI credentials or Application Default Credentials
 *
 * Before OCR/text parse, compares Telegram post id (data-post) with last run in Firestore
 * (populate_meta/telegram_sources) — same post → skip parsing and Firestore writes for that source.
 *
 * Usage:
 *   node functions/scripts/populate-firestore.cjs
 */

const admin = require("firebase-admin");
const { parseAllQueues } = require("../lib/parsers/cherkasyTelegramParser");
const {
  DtekTelegramParser,
  parseDtekChannelPosts,
  fingerprintDtekForRegion,
  fetchDtekChannelHtml,
} = require("../lib/parsers/dtekTelegramParser");
const {
  collectLvivScheduleCandidates,
  lvivCandidatesFingerprint,
} = require("../lib/lvivTelegramCandidates");
const {
  LvivTelegramParser,
  fetchLvivChannelHtml,
} = require("../lib/parsers/lvivTelegramParser");
const { pickCherkasyScheduleMessage } = require("../lib/cherkasyPostDate");
const { cherkasySchedulePostFingerprint } = require("../lib/telegramSourceFingerprints");
const { firestoreDocumentId } = require("../lib/documentId");
const { normalizeIntervalPairs, pairsToFlatMinutes } = require("../lib/normalizeIntervals");
const { kyivTodayYyyymmdd } = require("../lib/kyivDate");
const { applyScheduleDayWrite } = require("../lib/multiDayScheduleWrite");

const META_COLLECTION = "populate_meta";
const META_DOC_ID = "telegram_sources";

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

async function loadSourceFingerprints() {
  const snap = await db.collection(META_COLLECTION).doc(META_DOC_ID).get();
  return snap.exists ? snap.data() : {};
}

async function saveSourceFingerprint(field, value) {
  if (!value) return;
  await db.collection(META_COLLECTION).doc(META_DOC_ID).set({ [field]: value }, { merge: true });
}

async function writeSchedule(regionId, queueId, day, intervals) {
  const docId = firestoreDocumentId(regionId, queueId);
  const normalized = normalizeIntervalPairs(intervals);
  const flat = pairsToFlatMinutes(normalized);

  const result = await applyScheduleDayWrite(
    db,
    admin.messaging(),
    regionId,
    queueId,
    day,
    flat,
    new Date(),
  );

  if (result.skipped) {
    return { skipped: true, docId, version: undefined, slots: flat.length / 2 };
  }
  return {
    skipped: false,
    docId,
    version: result.version,
    slots: flat.length / 2,
  };
}

async function processCherkasy(day, prevFp) {
  console.log("\n=== Черкаси ===");
  try {
    const res = await fetch("https://t.me/s/pat_cherkasyoblenergo", {
      headers: { "User-Agent": "LightChecker-Populate/1.0" },
      signal: AbortSignal.timeout(15000),
    });
    const html = await res.text();

    const fp = cherkasySchedulePostFingerprint(html);
    if (fp && prevFp.cherkasy === fp) {
      console.log("  Той самий пост (data-post), пропуск парсингу");
      return;
    }

    const msgRe = /<div class="tgme_widget_message_text[^"]*"[^>]*>([\s\S]*?)<\/div>/g;
    const messages = [];
    let m;
    while ((m = msgRe.exec(html)) !== null) {
      messages.push(
        m[1].replace(/<br\s*\/?>/gi, "\n").replace(/<[^>]+>/g, "").replace(/&amp;/g, "&").trim()
      );
    }

    const picked = pickCherkasyScheduleMessage(messages);
    if (!picked) {
      console.log("  No schedule found");
      return;
    }

    const { text: scheduleMsg, dayYyyymmdd: dayForCherkasy } = picked;
    if (dayForCherkasy !== day) {
      console.log(`  Дата з поста: ${dayForCherkasy} (Kyiv today: ${day})`);
    }

    const allQueues = parseAllQueues(scheduleMsg);
    if (allQueues.size === 0) return;

    for (const [queueId, intervals] of allQueues) {
      const result = await writeSchedule("cherkasy", queueId, dayForCherkasy, intervals);
      const fmt = intervals.map(([s, e]) => `${formatMin(s)}-${formatMin(e)}`).join(", ");
      console.log(`  ${queueId}: ${fmt} → ${result.skipped ? "SKIP" : "v" + result.version}`);
    }

    if (fp) await saveSourceFingerprint("cherkasy", fp);
  } catch (e) {
    console.error("  Error:", e.message);
  }
}

async function processDtekRegions(day, prevFp) {
  console.log("\n=== ДТЕК (Київ / Одеса / Дніпро) — один запит HTML ===");
  try {
    const html = await fetchDtekChannelHtml();
    const posts = parseDtekChannelPosts(html);

    const cities = [
      { regionId: "kyiv", label: "Київ" },
      { regionId: "odesa", label: "Одеса" },
      { regionId: "dnipro", label: "Дніпро" },
    ];

    for (const { regionId, label } of cities) {
      const metaKey = `dtek_${regionId}`;
      const fp = fingerprintDtekForRegion(posts, regionId);

      if (fp && prevFp[metaKey] === fp) {
        console.log(`\n=== ${label} ===`);
        console.log("  Той самий пост (data-post), пропуск OCR");
        continue;
      }

      console.log(`\n=== ${label} ===`);
      const parser = new DtekTelegramParser(regionId);
      const { scheduleDayYyyymmdd, queues } = await parser.parseFromPosts(posts);
      if (queues.size === 0) {
        console.log("  No data found");
        continue;
      }

      const dayForDtek = scheduleDayYyyymmdd ?? day;
      if (scheduleDayYyyymmdd != null && scheduleDayYyyymmdd !== day) {
        console.log(`  Дата з тексту поста: ${scheduleDayYyyymmdd} (Kyiv today: ${day})`);
      }

      for (const [queueId, intervals] of queues) {
        const result = await writeSchedule(regionId, queueId, dayForDtek, intervals);
        const fmt = intervals.map(([s, e]) => `${formatMin(s)}-${formatMin(e)}`).join(", ");
        console.log(`  ${queueId}: ${fmt} → ${result.skipped ? "SKIP" : "v" + result.version}`);
      }

      if (fp) await saveSourceFingerprint(metaKey, fp);
    }
  } catch (e) {
    console.error("  Error:", e.message);
  }
}

async function processLviv(day, prevFp) {
  console.log("\n=== Львів ===");
  try {
    const html = await fetchLvivChannelHtml();
    const fp = lvivCandidatesFingerprint(html);
    if (fp && prevFp.lviv === fp) {
      console.log("  Той самий набір верхніх кандидат-постів, пропуск OCR");
      return;
    }

    const nCand = collectLvivScheduleCandidates(html).length;
    console.log(`  Кандидат-пости (до 5, від новішого): ${nCand}`);

    const parser = new LvivTelegramParser();
    const { scheduleDayYyyymmdd, queues, usedPostId } = await parser.parseFromHtml(html);
    if (queues.size === 0) {
      console.log("  No data for today/tomorrow in scanned posts (or OCR miss)");
      return;
    }

    if (usedPostId) {
      console.log(`  Прийнято графік з поста ${usedPostId} (дата в вікні Kyiv)`);
    }

    const dayForLviv = scheduleDayYyyymmdd ?? day;
    if (scheduleDayYyyymmdd != null && scheduleDayYyyymmdd !== day) {
      console.log(`  Дата з графіка (OCR): ${scheduleDayYyyymmdd} (Kyiv today: ${day})`);
    }

    for (const [queueId, intervals] of queues) {
      const result = await writeSchedule("lviv", queueId, dayForLviv, intervals);
      const fmt = intervals.map(([s, e]) => `${formatMin(s)}-${formatMin(e)}`).join(", ");
      console.log(`  ${queueId}: ${fmt} → ${result.skipped ? "SKIP" : "v" + result.version}`);
    }

    if (fp) await saveSourceFingerprint("lviv", fp);
  } catch (e) {
    console.error("  Error:", e.message);
  }
}

async function main() {
  const day = kyivTodayYyyymmdd();
  console.log("Date:", day, "(Kyiv time)");
  console.log("Project: lightchecker-ebe94");

  const prevFp = await loadSourceFingerprints();

  await processCherkasy(day, prevFp);
  await processDtekRegions(day, prevFp);
  await processLviv(day, prevFp);

  console.log("\nDone! Check Firestore: https://console.firebase.google.com/project/lightchecker-ebe94/firestore");
  process.exit(0);
}

main().catch((e) => {
  console.error("Fatal:", e);
  process.exit(1);
});
