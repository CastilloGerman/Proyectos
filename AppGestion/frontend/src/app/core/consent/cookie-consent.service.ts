import { Injectable, signal } from '@angular/core';

export type CookieConsentStatus = 'accepted' | 'rejected';

const STORAGE_KEY = 'cookie_consent';

/**
 * SEC-32: estado del consentimiento de cookies/analítica. La analítica de terceros solo se carga cuando el usuario
 * ha aceptado de forma explícita; "rechazar" y "sin decidir" equivalen a no cargarla.
 */
@Injectable({ providedIn: 'root' })
export class CookieConsentService {
  private readonly _status = signal<CookieConsentStatus | null>(this.read());

  /** `null` = todavía no ha decidido (se muestra el banner). */
  readonly status = this._status.asReadonly();

  accept(): void {
    this.write('accepted');
  }

  reject(): void {
    this.write('rejected');
  }

  private read(): CookieConsentStatus | null {
    try {
      const v = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null;
      return v === 'accepted' || v === 'rejected' ? v : null;
    } catch {
      return null;
    }
  }

  private write(value: CookieConsentStatus): void {
    try {
      localStorage.setItem(STORAGE_KEY, value);
    } catch {
      // almacenamiento no disponible: el estado vive solo en memoria durante la sesión
    }
    this._status.set(value);
  }
}
