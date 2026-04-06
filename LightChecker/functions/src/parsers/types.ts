export interface ScheduleParser {
  parse(data: string | Buffer): [number, number][];
}
