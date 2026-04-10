import { yyyymmddFromGraphicDayMonth } from "./kyivDate";

/** Родовий відмінок після «на» (наприклад «на 11 квітня»). */
const MONTH_GENITIVE_TO_NUM: Record<string, number> = {
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

function regionStartPattern(regionId: string): RegExp {
  switch (regionId) {
    case "kyiv":
      return /Київщин[аи]/iu;
    case "odesa":
      return /Одещин[аи]/iu;
    case "dnipro":
      return /Дніпропетровщин[аи]/iu;
    default:
      return /^/u;
  }
}

/** Фрагмент поста від заголовка регіону до наступного регіону ДТЕК (або весь текст, якщо заголовок не знайдено). */
export function sliceDtekTextForRegion(
  fullText: string,
  regionId: string,
): string {
  const startPat = regionStartPattern(regionId);
  const sm = startPat.exec(fullText);
  if (!sm || sm.index === undefined) return fullText;

  const start = sm.index;
  const after = fullText.slice(start + 1);
  const others = (["kyiv", "odesa", "dnipro"] as const).filter(
    (r) => r !== regionId,
  );
  let end = fullText.length;
  for (const oid of others) {
    const op = regionStartPattern(oid);
    const idx = after.search(op);
    if (idx >= 0) end = Math.min(end, start + 1 + idx);
  }
  return fullText.slice(start, end);
}

/**
 * Дата з тексту поста ДТЕК: «…графіки відключень на 11 квітня…» у блоці регіону.
 */
export function extractDtekScheduleDayYyyymmdd(
  postText: string,
  regionId: string,
  now = new Date(),
): number | null {
  const slice = sliceDtekTextForRegion(postText, regionId);
  const monthNames = Object.keys(MONTH_GENITIVE_TO_NUM).join("|");
  const re = new RegExp(
    `на\\s+(\\d{1,2})\\s+(${monthNames})(?:\\s+(\\d{4}))?`,
    "giu",
  );

  let last: RegExpExecArray | null = null;
  let m: RegExpExecArray | null;
  while ((m = re.exec(slice)) !== null) {
    last = m;
  }

  if (!last) {
    re.lastIndex = 0;
    while ((m = re.exec(postText)) !== null) {
      last = m;
    }
  }
  if (!last) return null;

  const day = parseInt(last[1], 10);
  const monthName = last[2].toLowerCase();
  const month = MONTH_GENITIVE_TO_NUM[monthName];
  const yearRaw = last[3];
  if (!month || day < 1 || day > 31) return null;

  if (yearRaw) {
    const y = parseInt(yearRaw, 10);
    if (y < 2000 || y > 2100) return null;
    return y * 10000 + month * 100 + day;
  }
  return yyyymmddFromGraphicDayMonth(day, month, now);
}
