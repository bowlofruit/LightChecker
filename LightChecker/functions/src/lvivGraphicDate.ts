import { yyyymmddFromGraphicDayMonth } from "./kyivDate";

/**
 * Дата типу 10.04 / 10.04.2026 з OCR графіка Львова (ДД.ММ).
 * Пропускає схожі на «1.1 чергу» збіги (обидва ≤6 і місяць ≤2 без року).
 */
export function extractLvivScheduleDayYyyymmdd(
  ocrText: string,
  now = new Date(),
): number | null {
  const normalized = ocrText
    .replace(/\u041E/g, "0")
    .replace(/\u043E/g, "0");

  const re = /\b(\d{1,2})[./](\d{1,2})(?:[./](\d{2,4}))?\b/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(normalized)) !== null) {
    const dd = parseInt(m[1], 10);
    const mm = parseInt(m[2], 10);
    const yyRaw = m[3];
    if (dd < 1 || dd > 31 || mm < 1 || mm > 12) continue;
    if (dd <= 6 && mm <= 2 && !yyRaw) continue;

    if (yyRaw) {
      const y =
        yyRaw.length === 2 ? 2000 + parseInt(yyRaw, 10) : parseInt(yyRaw, 10);
      if (y < 2000 || y > 2100) continue;
      return y * 10000 + mm * 100 + dd;
    }
    return yyyymmddFromGraphicDayMonth(dd, mm, now);
  }
  return null;
}
