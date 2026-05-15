import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { LanguageService, APP_LANGUAGE_STORAGE_KEY, SUPPORTED_UI_LANGUAGES } from './language.service';

describe('LanguageService', () => {
  describe('resolveInitialLanguage', () => {
    it('uses valid localStorage code', () => {
      const storage = { getItem: () => 'fr' } as Pick<Storage, 'getItem'>;
      expect(LanguageService.resolveInitialLanguage(storage, ['en-US'])).toBe('fr');
    });

    it('ignores invalid localStorage and uses browser list', () => {
      const storage = {
        getItem: (k: string) => (k === APP_LANGUAGE_STORAGE_KEY ? 'zz' : null),
      } as Pick<Storage, 'getItem'>;
      expect(LanguageService.resolveInitialLanguage(storage, ['en-GB', 'de'])).toBe('en');
    });

    it('maps en-US from navigator to en', () => {
      const storage = { getItem: () => null } as Pick<Storage, 'getItem'>;
      expect(LanguageService.resolveInitialLanguage(storage, ['en-US', 'fr'])).toBe('en');
    });

    it('normalizes ua to uk', () => {
      expect(LanguageService.normalizeLangCode('ua-UA')).toBe('uk');
    });

    it('falls back to es when nothing matches', () => {
      const storage = { getItem: () => null } as Pick<Storage, 'getItem'>;
      expect(LanguageService.resolveInitialLanguage(storage, ['xx', 'yy'])).toBe('es');
    });
  });

  describe('init', () => {
    function configureInitTest() {
      const use = vi.fn().mockReturnValue(of({}));
      const setTranslation = vi.fn();
      const setFallbackLang = vi.fn();
      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule],
        providers: [
          LanguageService,
          {
            provide: TranslateService,
            useValue: {
              addLangs: vi.fn(),
              setTranslation,
              setFallbackLang,
              use,
            },
          },
        ],
      });
      return {
        http: TestBed.inject(HttpTestingController),
        svc: TestBed.inject(LanguageService),
        setTranslation,
        setFallbackLang,
        use,
      };
    }

    beforeEach(() => {
      vi.stubGlobal(
        'localStorage',
        {
          getItem: () => null,
          setItem: vi.fn(),
          removeItem: vi.fn(),
        } as unknown as Storage,
      );
      vi.stubGlobal('navigator', { languages: ['ro-RO'] });
    });

    it('preloads all locale files, registers translations, then applies resolved language', async () => {
      const { http, svc, setTranslation, use } = configureInitTest();
      const done = svc.init();
      for (const code of SUPPORTED_UI_LANGUAGES) {
        http.expectOne((req) => req.urlWithParams.startsWith(`assets/i18n/${code}.json?v=`)).flush({ x: 1 });
      }
      await done;
      expect(setTranslation).toHaveBeenCalledTimes(SUPPORTED_UI_LANGUAGES.length);
      expect(use).toHaveBeenCalledWith('ro');
      http.verify();
    });

    it('does not reject startup when a non-active locale fails to preload', async () => {
      const { http, svc, setTranslation, use } = configureInitTest();
      const done = svc.init();
      for (const code of SUPPORTED_UI_LANGUAGES) {
        const req = http.expectOne((r) => r.urlWithParams.startsWith(`assets/i18n/${code}.json?v=`));
        if (code === 'fr') {
          req.flush('missing', { status: 404, statusText: 'Not Found' });
        } else {
          req.flush({ x: code });
        }
      }
      await done;
      expect(setTranslation).toHaveBeenCalledTimes(SUPPORTED_UI_LANGUAGES.length - 1);
      expect(use).toHaveBeenCalledWith('ro');
      http.verify();
    });

    it('falls back to Spanish when the resolved locale fails to preload', async () => {
      const { http, svc, use } = configureInitTest();
      const done = svc.init();
      for (const code of SUPPORTED_UI_LANGUAGES) {
        const req = http.expectOne((r) => r.urlWithParams.startsWith(`assets/i18n/${code}.json?v=`));
        if (code === 'ro') {
          req.flush('broken', { status: 500, statusText: 'Server Error' });
        } else {
          req.flush({ x: code });
        }
      }
      await done;
      expect(use).toHaveBeenCalledWith('es');
      http.verify();
    });
  });
});
