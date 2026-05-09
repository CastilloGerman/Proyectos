import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

/** Next rollout: translate feature modules with prefixes (auth.*, dashboard.*, …); keep API or server strings untranslated. */
export const APP_LANGUAGE_STORAGE_KEY = 'app_language';

/** Ukrainian uses ISO 639-1 `uk`; UI label for the switcher remains `UK` per product spec. */
export const SUPPORTED_UI_LANGUAGES = ['es', 'en', 'fr', 'ro', 'uk'] as const;

export type SupportedUiLanguage = (typeof SUPPORTED_UI_LANGUAGES)[number];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);

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

  init(): Promise<void> {
    const initial = LanguageService.resolveInitialLanguage(
      typeof localStorage !== 'undefined' ? localStorage : null,
      typeof navigator !== 'undefined' ? navigator.languages : null,
    );
    this.translate.addLangs([...SUPPORTED_UI_LANGUAGES]);
    this.translate.setFallbackLang('es');
    return firstValueFrom(this.translate.use(initial)).then(() => undefined);
  }

  setLanguage(lang: string): void {
    const code = LanguageService.normalizeLangCode(lang);
    if (!LanguageService.isSupported(code)) {
      return;
    }
    void firstValueFrom(this.translate.use(code));
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(APP_LANGUAGE_STORAGE_KEY, code);
    }
  }
}
