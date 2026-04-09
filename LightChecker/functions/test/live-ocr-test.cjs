/**
 * Live OCR test: fetch image from Telegram → Tesseract OCR → parse.
 *
 * Usage:
 *   node functions/test/live-ocr-test.cjs                    # auto-find from channel
 *   node functions/test/live-ocr-test.cjs https://...jpg     # direct image URL
 *   node functions/test/live-ocr-test.cjs ./local-image.jpg  # local file
 */

const fs = require("fs");
const { OcrImageParser } = require("../lib/parsers/ocrImageParser");

async function main() {
  const arg = process.argv[2];
  let buffer;

  if (arg && fs.existsSync(arg)) {
    // Local file
    console.log(`Reading local file: ${arg}`);
    buffer = fs.readFileSync(arg);
  } else if (arg && arg.startsWith("http")) {
    // Direct URL
    console.log(`Downloading: ${arg}`);
    const res = await fetch(arg, { signal: AbortSignal.timeout(15000) });
    buffer = Buffer.from(await res.arrayBuffer());
  } else {
    // Try to find images from Lviv oblenergo channel
    const channelUrl = "https://t.me/s/lvivoblenergo";
    console.log(`Fetching channel: ${channelUrl}`);
    const res = await fetch(channelUrl, {
      headers: { "User-Agent": "LightChecker-Test/1.0" },
      signal: AbortSignal.timeout(15000),
    });
    const html = await res.text();

    // Find all image URLs from posts (Telegram CDN)
    const imgUrls = [];
    const re = /https:\/\/cdn[^"'\s)]+\.(?:jpg|jpeg|png|webp)/gi;
    let m;
    while ((m = re.exec(html)) !== null) {
      if (!m[0].includes("emoji") && !m[0].includes("avatar")) {
        imgUrls.push(m[0]);
      }
    }

    if (imgUrls.length === 0) {
      console.error("No post images found in channel HTML.");
      console.log("\nTry downloading the image manually and run:");
      console.log("  node functions/test/live-ocr-test.cjs ./schedule.jpg");
      process.exit(1);
    }

    // Use the last image (most recent post)
    const imgUrl = imgUrls[imgUrls.length - 1];
    console.log(`Found ${imgUrls.length} images. Using latest: ${imgUrl}`);
    const imgRes = await fetch(imgUrl, { signal: AbortSignal.timeout(15000) });
    buffer = Buffer.from(await imgRes.arrayBuffer());
  }

  console.log(`Image size: ${buffer.length} bytes\n`);
  console.log("Running OCR (may take 30-60 seconds)...\n");

  const parser = new OcrImageParser();

  const intervals = await parser.parseAsync(buffer);
  console.log(`=== All intervals found: ${intervals.length} ===`);
  intervals.forEach(([s, e]) => {
    const sh = String(Math.floor(s / 60)).padStart(2, "0");
    const sm = String(s % 60).padStart(2, "0");
    const eh = String(Math.floor(e / 60)).padStart(2, "0");
    const em = String(e % 60).padStart(2, "0");
    console.log(`  ${sh}:${sm} - ${eh}:${em}`);
  });

  console.log("\n=== Per-queue intervals ===");
  const queues = await parser.parseAllQueuesAsync(buffer);
  if (queues.size === 0) {
    console.log("  (no queue-specific lines — image may use table format)");
    console.log("  All intervals above apply to all queues.");
  }
  for (const [queueId, qIntervals] of queues) {
    const formatted = qIntervals
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
}

main().catch(console.error);
