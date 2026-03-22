/** Lee el claim `sid` del JWT (sin verificar firma; solo para UI / caché local). */
export function readJwtSessionId(token: string | null | undefined): string | undefined {
  if (!token || !token.includes('.')) {
    return undefined;
  }
  try {
    const part = token.split('.')[1];
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(atob(base64));
    const sid = json.sid;
    return typeof sid === 'string' && sid.trim().length > 0 ? sid.trim() : undefined;
  } catch {
    return undefined;
  }
}
