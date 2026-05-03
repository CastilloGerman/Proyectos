import { HttpErrorResponse } from '@angular/common/http';

/**
 * Mensaje usable para snackbars a partir del cuerpo de error típico de la API Spring / JSON `{ error }`.
 */
export function messageFromHttpError(err: unknown, fallback: string): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    if (typeof body === 'string' && body.trim().length > 0) {
      return body.length > 200 ? fallback : body.trim();
    }
    if (body && typeof body === 'object') {
      const o = body as Record<string, unknown>;
      for (const k of ['error', 'message', 'detail', 'title']) {
        const v = o[k];
        if (typeof v === 'string' && v.trim().length > 0) return v.trim();
      }
    }
    if (err.status === 0) {
      return 'Sin conexión con el servidor. Comprueba la red y que la API esté disponible.';
    }
    if (err.status >= 500) {
      return 'El servidor respondió con un error. Inténtalo más tarde.';
    }
  }
  return fallback;
}
