/** YYYYMMDD у календарі Europe/Kyiv (узгоджено з Android `KyivTime`). */
function formatYyyyMmDdEuropeKyiv(date: Date): number {
  const fmt = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Kyiv",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });
  const parts = fmt.formatToParts(date);
  const y = parts.find((p) => p.type === "year")!.value;
  const m = parts.find((p) => p.type === "month")!.value;
  const d = parts.find((p) => p.type === "day")!.value;
  return Number(`${y}${m}${d}`);
}

export function kyivTodayYyyymmdd(now = new Date()): number {
  return formatYyyyMmDdEuropeKyiv(now);
}

/**
 * Наступний календарний день у Kyiv (наближено: +24h від полудня UTC поточного Kyiv-дня;
 * для production-парсера краще спільна бібліотека з TZ).
 */
export function kyivTomorrowYyyymmdd(now = new Date()): number {
  const today = kyivTodayYyyymmdd(now);
  const y = Math.floor(today / 10000);
  const mo = Math.floor((today % 10000) / 100);
  const da = today % 100;
  const noonUtc = Date.UTC(y, mo - 1, da, 12, 0, 0);
  return formatYyyyMmDdEuropeKyiv(new Date(noonUtc + 24 * 60 * 60 * 1000));
}
