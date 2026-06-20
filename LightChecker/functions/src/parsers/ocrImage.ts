import sharp from "sharp";
import { createWorker } from "tesseract.js";

/**
 * Preprocess an image (greyscale → normalize → sharpen → threshold → 2x upscale) and run
 * Ukrainian+English OCR.
 *
 * Creates a dedicated Tesseract worker per call and ALWAYS terminates it in `finally`,
 * so worker processes / WASM heaps don't accumulate across the many images processed in
 * a single 1 GiB / 300 s `updateAllCities` invocation.
 */
export async function ocrImage(raw: Buffer): Promise<string> {
  const processed = await sharp(raw)
    .greyscale()
    .normalize()
    .sharpen({ sigma: 2 })
    .threshold(128)
    .resize({ width: 2000, withoutEnlargement: false })
    .png()
    .toBuffer();

  const worker = await createWorker("ukr+eng", undefined, { logger: () => {} });
  try {
    const {
      data: { text },
    } = await worker.recognize(processed);
    return text;
  } finally {
    await worker.terminate();
  }
}
