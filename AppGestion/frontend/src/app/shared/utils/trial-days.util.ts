/**
 * Días de calendario desde hoy (medianoche local) hasta la fecha indicada (medianoche local).
 * @returns null si no hay fecha o no es parseable.
 */
export function daysFromTodayToDateEnd(dateIso: string | undefined | null): number | null {
  if (!dateIso) return null;
  const endDate = new Date(dateIso);
  if (Number.isNaN(endDate.getTime())) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  endDate.setHours(0, 0, 0, 0);
  return Math.ceil((endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
}
