import { Injectable, computed, signal } from '@angular/core';

const STORAGE_KEY = 'appgestion-theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _dark = signal(false);

  /** Modo oscuro activo (solo UI local; se guarda en este navegador). */
  readonly isDark = computed(() => this._dark());

  constructor() {
    this.syncFromStorage();
  }

  /** Alinea estado y clase en <html> con localStorage. */
  syncFromStorage(): void {
    if (typeof localStorage === 'undefined') {
      return;
    }
    try {
      const dark = localStorage.getItem(STORAGE_KEY) === 'dark';
      this._dark.set(dark);
      this.applyDocumentClass(dark);
    } catch {
      /* private mode / quota */
    }
  }

  setDark(dark: boolean): void {
    this._dark.set(dark);
    this.applyDocumentClass(dark);
    try {
      localStorage.setItem(STORAGE_KEY, dark ? 'dark' : 'light');
    } catch {
      /* ignore */
    }
  }

  toggle(): void {
    this.setDark(!this._dark());
  }

  private applyDocumentClass(dark: boolean): void {
    if (typeof document === 'undefined') {
      return;
    }
    document.documentElement.classList.toggle('app-dark-theme', dark);
  }
}
