import { HttpErrorResponse } from '@angular/common/http';

/** Textos opcionales para errores de red/servidor (p. ej. desde i18n). */
export interface HttpErrorPresetMessages {
  offline?: string;
  server?: string;
}

/**
 * Mensaje usable para snackbars a partir del cuerpo de error típico de la API (`error`, `message`, etc.).
 */
export function messageFromHttpError(
  err: unknown,
  fallback: string,
  presets?: HttpErrorPresetMessages
): string {
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
      return (
        presets?.offline ??
        'Sin conexión a internet. Comprueba tu red e inténtalo de nuevo.'
      );
    }
    if (err.status >= 500) {
      return (
        presets?.server ??
        'Algo falló por nuestra parte. Inténtalo de nuevo dentro de unos minutos.'
      );
    }
  }
  return fallback;
}
