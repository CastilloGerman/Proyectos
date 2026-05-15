import { HttpClient } from '@angular/common/http';
import { Injectable, NgZone, inject } from '@angular/core';
import { TranslateService, TranslationObject } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

/** Next rollout: translate feature modules with prefixes (auth.*, dashboard.*, …); keep API or server strings untranslated. */
export const APP_LANGUAGE_STORAGE_KEY = 'app_language';

/** Ukrainian uses ISO 639-1 `uk`; UI label for the switcher remains `UK` per product spec. */
export const SUPPORTED_UI_LANGUAGES = ['es', 'en', 'fr', 'ro', 'uk'] as const;
const I18N_ASSET_VERSION = '20260509-about-i18n';

export type SupportedUiLanguage = (typeof SUPPORTED_UI_LANGUAGES)[number];

/** Load JSON with a path relative to the app base (avoids /assets vs subpath deploy issues). */
const i18nAsset = (code: string) => `assets/i18n/${code}.json?v=${I18N_ASSET_VERSION}`;

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly http = inject(HttpClient);
  private readonly translate = inject(TranslateService);
  private readonly ngZone = inject(NgZone);

  /** Resolve language without DI — used by tests and `init`. */
  static resolveInitialLanguage(
    storage: Pick<Storage, 'getItem'> | null,
    browserLanguages: readonly string[] | null,
  ): SupportedUiLanguage {
    const fromStorage = storage?.getItem(APP_LANGUAGE_STORAGE_KEY)?.trim().toLowerCase();
    if (fromStorage && LanguageService.isSupported(fromStorage)) {
      return fromStorage;
    }
    if (browserLanguages?.length) {
      for (const raw of browserLanguages) {
        const code = LanguageService.normalizeLangCode(raw);
        if (LanguageService.isSupported(code)) {
          return code;
        }
      }
    }
    return 'es';
  }

  static normalizeLangCode(raw: string): string {
    const part = raw.split(/[-_]/)[0]?.trim().toLowerCase() ?? '';
    if (part === 'ua') {
      return 'uk';
    }
    return part;
  }

  static isSupported(code: string): code is SupportedUiLanguage {
    return (SUPPORTED_UI_LANGUAGES as readonly string[]).includes(code);
  }

  /**
   * Precarga todos los idiomas y registra traducciones antes de `use()`, evitando la condición de carrera
   * de ngx-translate (changeLang con HTTP raw antes de setTranslations → claves sin traducir en pantalla).
   */
  init(): Promise<void> {
    const initial = LanguageService.resolveInitialLanguage(
      typeof localStorage !== 'undefined' ? localStorage : null,
      typeof navigator !== 'undefined' ? navigator.languages : null,
    );
    this.translate.addLangs([...SUPPORTED_UI_LANGUAGES]);

    return Promise.all(
      SUPPORTED_UI_LANGUAGES.map((code) =>
        firstValueFrom(this.http.get<TranslationObject>(i18nAsset(code)))
          .then((data) => {
            this.translate.setTranslation(code, data);
            return code;
          })
          .catch((err: unknown) => {
            console.warn('[i18n] Failed to preload translations for', code, err);
            return null;
          }),
      ),
    )
      .then((loadedCodes) => {
        const loaded = new Set(loadedCodes.filter((code): code is SupportedUiLanguage => code !== null));
        if (loaded.size === 0) {
          throw new Error('No i18n translations could be loaded');
        }
        const firstLoaded = Array.from(loaded)[0] ?? 'es';
        const active: SupportedUiLanguage = loaded.has(initial) ? initial : loaded.has('es') ? 'es' : firstLoaded;
        this.translate.setFallbackLang('es');
        return firstValueFrom(this.translate.use(active));
      })
      .then(() => {
        this.ngZone.run(() => undefined);
      });
  }

  /**
   * Los JSON ya están en memoria tras `init`; `use` aplica el idioma de forma síncrona (sin carrera).
   */
  setLanguage(lang: string): void {
    const code = LanguageService.normalizeLangCode(lang);
    if (!LanguageService.isSupported(code)) {
      return;
    }
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(APP_LANGUAGE_STORAGE_KEY, code);
    }
    void firstValueFrom(this.translate.use(code))
      .then(() => this.ngZone.run(() => undefined))
      .catch((err: unknown) => console.warn('[i18n] Failed to switch language', code, err));
  }
}
