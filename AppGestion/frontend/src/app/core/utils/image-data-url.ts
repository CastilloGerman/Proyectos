/** Data URL para vista previa a partir del Base64 crudo que devuelve la API. */
export function dataUrlFromStoredBase64(b64: string | null | undefined): string | null {
  if (!b64) return null;
  const s = b64.trim();
  if (s.startsWith('/9j/') || s.startsWith('/9j')) return 'data:image/jpeg;base64,' + s;
  if (s.startsWith('iVBORw0KGgo')) return 'data:image/png;base64,' + s;
  if (s.startsWith('R0lGOD')) return 'data:image/gif;base64,' + s;
  if (s.startsWith('UklGR')) return 'data:image/webp;base64,' + s;
  return 'data:image/png;base64,' + s;
}
