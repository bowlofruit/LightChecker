import { ScheduleParser } from "./types";

/**
 * OCR-based parser for image schedules (PNG/JPEG).
 * Requires @google-cloud/vision. Currently a stub.
 */
export class OcrImageParser implements ScheduleParser {
  parse(_data: string | Buffer): [number, number][] {
    // TODO: Integrate Google Cloud Vision API
    // 1. Send image buffer to Vision API TEXT_DETECTION
    // 2. Extract recognized text
    // 3. Apply regex parsing to extract time intervals
    return [];
  }
}
