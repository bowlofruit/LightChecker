export type ValidateSlotsResult =
  | { ok: true }
  | { ok: false; reason: string };

const MIN_D = 20_200_101;
const MAX_D = 20_991_231;

export function validateSchedulePayload(
  schemaVersion: number,
  version: number,
  dayYyyymmdd: number,
  slotMinutes: number[],
): ValidateSlotsResult {
  if (schemaVersion !== 1) {
    return { ok: false, reason: `unsupported_schema_f=${schemaVersion}` };
  }
  if (dayYyyymmdd < MIN_D || dayYyyymmdd > MAX_D) {
    return { ok: false, reason: `invalid_d=${dayYyyymmdd}` };
  }
  if (version < 1) {
    return { ok: false, reason: "invalid_v" };
  }
  if (slotMinutes.length % 2 !== 0) {
    return { ok: false, reason: "s_not_pairs" };
  }
  for (let i = 0; i < slotMinutes.length; i += 2) {
    const a = slotMinutes[i]!;
    const b = slotMinutes[i + 1]!;
    if (a < 0 || a > 1440 || b < 0 || b > 1440 || a > b) {
      return { ok: false, reason: `invalid_slot_${a},${b}` };
    }
  }
  return { ok: true };
}
