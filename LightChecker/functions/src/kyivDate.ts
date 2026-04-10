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

function yyyymmddUtcMidnight(n: number): number {
  const y = Math.floor(n / 10000);
  const m = Math.floor((n % 10000) / 100) - 1;
  const d = n % 100;
  return Date.UTC(y, m, d);
}

/**
 * День і місяць з графіка (формат ДД.ММ), рік — з поточної дати в Kyiv зі зсувом ±1 рік,
 * якщо інакше дата виходить >90 днів від «сьогодні» (кінець/початок року).
 */
export function yyyymmddFromGraphicDayMonth(
  day: number,
  month: number,
  now = new Date(),
): number {
  const todayN = kyivTodayYyyymmdd(now);
  let y = Math.floor(todayN / 10000);
  let candidate = y * 10000 + month * 100 + day;
  const tMs = yyyymmddUtcMidnight(todayN);
  let cMs = yyyymmddUtcMidnight(candidate);
  let dayDiff = (cMs - tMs) / 86400000;
  if (dayDiff > 90) y -= 1;
  else if (dayDiff < -90) y += 1;
  return y * 10000 + month * 100 + day;
}
