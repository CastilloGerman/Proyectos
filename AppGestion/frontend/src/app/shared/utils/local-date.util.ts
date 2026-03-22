/** Parsea `YYYY-MM-DD` del API a `Date` en calendario local (evita desfases por UTC). */
export function parseApiLocalDate(iso: string | null | undefined): Date | null {
  if (!iso) {
    return null;
  }
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso.trim());
  if (!m) {
    return null;
  }
  const y = Number(m[1]);
  const mo = Number(m[2]);
  const d = Number(m[3]);
  return new Date(y, mo - 1, d);
}

/** Serializa `Date` local a `YYYY-MM-DD` para el API. */
export function formatLocalDateForApi(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
