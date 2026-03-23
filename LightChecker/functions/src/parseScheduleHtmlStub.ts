/**
 * Мінімальний парсер інтервалів HH:mm–HH:mm з HTML (fn-parse-schedule stub).
 */
export function parseScheduleHtmlStub(html: string): [number, number][] {
  const re = /(\d{1,2}):(\d{2})\s*[\u2013\u2212-]\s*(\d{1,2}):(\d{2})/g;
  const out: [number, number][] = [];
  let m: RegExpExecArray | null;
  while ((m = re.exec(html)) !== null) {
    const start = Number(m[1]) * 60 + Number(m[2]);
    const end = Number(m[3]) * 60 + Number(m[4]);
    if (start >= 0 && end <= 1440 && start <= end) {
      out.push([start, end]);
    }
  }
  return out;
}
