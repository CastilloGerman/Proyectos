function readJwtPayload(token: string | null | undefined): Record<string, unknown> | undefined {
  if (!token || !token.includes('.')) {
    return undefined;
  }
  try {
    const part = token.split('.')[1];
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/');
    const json = JSON.parse(atob(base64));
    return json !== null && typeof json === 'object' ? (json as Record<string, unknown>) : undefined;
  } catch {
    return undefined;
  }
}

/** Lee el claim `sid` del JWT (sin verificar firma; solo para UI / caché local). */
export function readJwtSessionId(token: string | null | undefined): string | undefined {
  const sid = readJwtPayload(token)?.['sid'];
  return typeof sid === 'string' && sid.trim().length > 0 ? sid.trim() : undefined;
}

/** `exp` en segundos UNIX. Undefined si el token no trae un exp numérico. */
export function readJwtExpSeconds(token: string | null | undefined): number | undefined {
  const exp = readJwtPayload(token)?.['exp'];
  return typeof exp === 'number' && Number.isFinite(exp) ? exp : undefined;
}

/**
 * Caducado solo cuando hay `exp` y ya pasó. Sin ese claim se deja que lo decida el servidor:
 * un token antiguo no debe cerrar la sesión en el cliente por no poder leerse.
 */
export function isJwtExpired(token: string | null | undefined, nowMs = Date.now()): boolean {
  const exp = readJwtExpSeconds(token);
  if (exp == null) {
    return false;
  }
  return exp * 1000 <= nowMs;
}
