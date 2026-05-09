import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { LanguageService, APP_LANGUAGE_STORAGE_KEY } from './language.service';

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

    it('configures translate and applies resolved language', async () => {
      const use = vi.fn().mockReturnValue(of({}));
      TestBed.configureTestingModule({
        providers: [
          LanguageService,
          {
            provide: TranslateService,
            useValue: {
              addLangs: vi.fn(),
              setFallbackLang: vi.fn(),
              use,
            },
          },
        ],
      });
      const svc = TestBed.inject(LanguageService);
      await svc.init();
      expect(use).toHaveBeenCalledWith('ro');
    });
  });
});
