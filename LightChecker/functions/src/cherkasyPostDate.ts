import {
  kyivTodayYyyymmdd,
  kyivTomorrowYyyymmdd,
  yyyymmddFromGraphicDayMonth,
} from "./kyivDate";

/** Родовий відмінок місяця, як у «на 10 квітня». */
const MONTH_GENITIVE: Record<string, number> = {
  січня: 1,
  лютого: 2,
  березня: 3,
  квітня: 4,
  травня: 5,
  червня: 6,
  липня: 7,
  серпня: 8,
  вересня: 9,
  жовтня: 10,
  листопада: 11,
  грудня: 12,
};

/**
 * Дата з тексту поста Черкас («… на 10 квітня …», «10 квітня 2026»).
 * Без року — через yyyymmddFromGraphicDayMonth відносно Kyiv «сьогодні».
 */
export function extractCherkasyScheduleDayYyyymmdd(
  text: string,
  now: Date = new Date(),
): number | null {
  const norm = text.replace(/\u00a0/g, " ");
  const re =
    /(\d{1,2})\s+(січня|лютого|березня|квітня|травня|червня|липня|серпня|вересня|жовтня|листопада|грудня)(?:\s+р(?:оку)?)?(?:\s+(\d{4}))?/gi;
  const m = re.exec(norm);
  if (!m) return null;
  const day = parseInt(m[1], 10);
  const monthName = m[2].toLowerCase();
  const month = MONTH_GENITIVE[monthName];
  if (month == null || day < 1 || day > 31) return null;
  if (m[3]) {
    const y = parseInt(m[3], 10);
    return y * 10000 + month * 100 + day;
  }
  return yyyymmddFromGraphicDayMonth(day, month, now);
}

/**
 * Серед постів з рядками черг обирає текст і календарний день.
 * Спочатку (від новішого) береться пост, дата якого ∈ {сьогодні, завтра} за Kyiv —
 * щоб оновлення «за вчора» не перекривало актуальний графік на сьогодні.
 */
export function pickCherkasyScheduleMessage(
  messages: string[],
  now: Date = new Date(),
): { text: string; dayYyyymmdd: number } | null {
  const candidates = messages.filter((msg) =>
    /\d+\.\d+\s+\d{1,2}:\d{2}/.test(msg),
  );
  if (candidates.length === 0) return null;

  const today = kyivTodayYyyymmdd(now);
  const tomorrow = kyivTomorrowYyyymmdd(now);
  const allowed = new Set([String(today), String(tomorrow)]);

  const newestFirst = [...candidates].reverse();

  for (const text of newestFirst) {
    const parsed = extractCherkasyScheduleDayYyyymmdd(text, now);
    const dayForWindow = parsed ?? today;
    if (allowed.has(String(dayForWindow))) {
      return { text, dayYyyymmdd: parsed ?? today };
    }
  }

  const fallback = newestFirst[0]!;
  const d = extractCherkasyScheduleDayYyyymmdd(fallback, now) ?? today;
  return { text: fallback, dayYyyymmdd: d };
}
