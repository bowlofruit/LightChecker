/** Сирі пари [start,end] у хвилинах від півночі → відсортовано та злито перетини/дотики. */
export function normalizeIntervalPairs(pairs: [number, number][]): [number, number][] {
  if (pairs.length === 0) {
    return [];
  }
  const sorted = [...pairs].sort((a, b) => a[0] - b[0]);
  const merged: [number, number][] = [];
  let cur: [number, number] = sorted[0]!;
  for (let i = 1; i < sorted.length; i++) {
    const next = sorted[i]!;
    if (next[0] <= cur[1]) {
      cur = [cur[0], Math.max(cur[1], next[1])];
    } else {
      merged.push(cur);
      cur = next;
    }
  }
  merged.push(cur);
  return merged;
}

/** Пари → плоский масив [a0,b0,…] */
export function pairsToFlatMinutes(pairs: [number, number][]): number[] {
  const out: number[] = [];
  for (const [a, b] of pairs) {
    out.push(a, b);
  }
  return out;
}
