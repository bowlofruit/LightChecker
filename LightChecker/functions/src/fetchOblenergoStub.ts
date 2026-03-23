const DEFAULT_STUB_HTML = `<!DOCTYPE html><html><body>
<p>08:00–10:00</p>
<p>14:00-15:30</p>
</body></html>`;

/**
 * Завантаження HTML або заглушка (fn-fetch-oblenergo-stub).
 */
export async function fetchOblenergoStub(url: string): Promise<string> {
  const u = url.trim();
  if (!u) {
    return DEFAULT_STUB_HTML;
  }
  const res = await fetch(u, { redirect: "follow" });
  if (!res.ok) {
    throw new Error(`oblenergo_fetch_status_${res.status}`);
  }
  return res.text();
}
