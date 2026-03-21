import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extrae un mensaje legible de respuestas Spring (message, detail, error) o red genérica.
 */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (!(err instanceof HttpErrorResponse)) {
    return fallback;
  }
  const e = err.error;
  if (typeof e === 'string' && e.trim()) {
    return e.trim();
  }
  if (e && typeof e === 'object') {
    const o = e as Record<string, unknown>;
    for (const key of ['message', 'detail', 'error', 'title'] as const) {
      const v = o[key];
      if (typeof v === 'string' && v.trim()) {
        return v.trim();
      }
    }
  }
  if (err.status === 0) {
    return 'Sin conexión con el servidor. Comprueba la red o que la API esté en marcha.';
  }
  if (err.status === 401) {
    return 'Sesión no válida o expirada. Vuelve a iniciar sesión.';
  }
  if (err.status === 403) {
    return 'No tienes permiso para realizar esta acción.';
  }
  if (err.status >= 500) {
    return 'Error en el servidor. Inténtalo más tarde.';
  }
  return fallback;
}
