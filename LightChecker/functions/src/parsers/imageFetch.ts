/** Hard cap on OCR image size to bound memory under the 1 GiB invocation budget. */
const MAX_IMAGE_BYTES = 8 * 1024 * 1024;

/**
 * Fetch a remote image for OCR with basic hardening. Image URLs are scraped from
 * third-party Telegram HTML, so we validate before buffering: https-only scheme,
 * HTTP success, an `image/*` content-type, and a size cap (declared + actual).
 */
export async function fetchImage(url: string): Promise<Buffer> {
  if (!/^https:\/\//i.test(url)) {
    throw new Error("image_fetch_bad_scheme");
  }
  const res = await fetch(url, { signal: AbortSignal.timeout(15000) });
  if (!res.ok) {
    throw new Error(`image_fetch_status_${res.status}`);
  }
  const contentType = res.headers.get("content-type") ?? "";
  if (!contentType.toLowerCase().startsWith("image/")) {
    throw new Error(`image_fetch_not_image_${contentType}`);
  }
  const declaredLength = Number(res.headers.get("content-length") ?? "0");
  if (declaredLength > MAX_IMAGE_BYTES) {
    throw new Error(`image_fetch_too_large_${declaredLength}`);
  }
  const buf = Buffer.from(await res.arrayBuffer());
  if (buf.length > MAX_IMAGE_BYTES) {
    throw new Error(`image_fetch_too_large_${buf.length}`);
  }
  return buf;
}
