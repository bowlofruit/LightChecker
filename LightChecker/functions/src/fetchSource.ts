/**
 * Generic HTTP fetch with timeout and user-agent.
 * Returns the response body as string, or null on failure.
 */
export async function fetchSource(url: string): Promise<string | null> {
  if (!url) return null;
  try {
    const response = await fetch(url, {
      headers: { "User-Agent": "LightChecker-Functions/1.0" },
      signal: AbortSignal.timeout(15_000),
      redirect: "follow",
    });
    if (!response.ok) return null;
    return await response.text();
  } catch {
    return null;
  }
}
