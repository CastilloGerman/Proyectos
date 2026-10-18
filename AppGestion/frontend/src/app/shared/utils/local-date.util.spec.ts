import { formatLocalDateForApi, parseApiLocalDate } from './local-date.util';

describe('local-date.util', () => {
  describe('formatLocalDateForApi', () => {
    it('keeps the calendar day for local midnight (avoids UTC toISOString shift)', () => {
      // Material datepicker typically yields local midnight. In UTC+ timezones,
      // Date#toISOString().slice(0, 10) moves the day back (e.g. 1 Apr → 31 Mar),
      // which would mis-file gastos into the previous Modelo 303 quarter.
      const aprilFirstLocalMidnight = new Date(2026, 3, 1, 0, 0, 0, 0);
      expect(formatLocalDateForApi(aprilFirstLocalMidnight)).toBe('2026-04-01');

      const isoSlice = aprilFirstLocalMidnight.toISOString().slice(0, 10);
      if (aprilFirstLocalMidnight.getTimezoneOffset() < 0) {
        expect(isoSlice).toBe('2026-03-31');
      }
    });

    it('formats year-end and year-start without shifting the year', () => {
      expect(formatLocalDateForApi(new Date(2026, 0, 1, 0, 0, 0, 0))).toBe('2026-01-01');
      expect(formatLocalDateForApi(new Date(2026, 11, 31, 0, 0, 0, 0))).toBe('2026-12-31');
    });
  });

  describe('parseApiLocalDate', () => {
    it('parses API date-only strings as local calendar dates', () => {
      const d = parseApiLocalDate('2026-04-01');
      expect(d).not.toBeNull();
      expect(d!.getFullYear()).toBe(2026);
      expect(d!.getMonth()).toBe(3);
      expect(d!.getDate()).toBe(1);
    });

    it('returns null for empty or invalid values', () => {
      expect(parseApiLocalDate(null)).toBeNull();
      expect(parseApiLocalDate(undefined)).toBeNull();
      expect(parseApiLocalDate('')).toBeNull();
      expect(parseApiLocalDate('01-04-2026')).toBeNull();
    });
  });
});
