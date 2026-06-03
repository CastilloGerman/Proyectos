export const LEGAL_PATHS = [
  '/privacidad',
  '/terminos',
  '/cookies',
  '/aviso-legal',
  '/reembolsos',
] as const;

export function isLegalPath(url: string): boolean {
  const path = url.split('?')[0].split('#')[0];
  return LEGAL_PATHS.some((p) => path === p);
}
