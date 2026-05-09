import { Component, ViewChild, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { map, startWith } from 'rxjs/operators';
import { LanguageService, SUPPORTED_UI_LANGUAGES, SupportedUiLanguage } from '../../core/i18n/language.service';

type LangMeta = { short: string; native: string };

const LANG_DISPLAY: Record<SupportedUiLanguage, LangMeta> = {
  es: { short: 'ES', native: 'Español' },
  en: { short: 'EN', native: 'English' },
  fr: { short: 'FR', native: 'Français' },
  ro: { short: 'RO', native: 'Română' },
  uk: { short: 'UK', native: 'Українська' },
};

const FLAG_MAP: Record<SupportedUiLanguage, string> = {
  es: 'lang-flag lang-flag--es',
  en: 'lang-flag lang-flag--en',
  fr: 'lang-flag lang-flag--fr',
  ro: 'lang-flag lang-flag--ro',
  uk: 'lang-flag lang-flag--uk',
};

@Component({
  selector: 'app-language-switcher',
  standalone: true,
  imports: [MatButtonModule, MatMenuModule, MatIconModule, TranslateModule],
  templateUrl: './language-switcher.component.html',
  styleUrl: './language-switcher.component.scss',
})
export class LanguageSwitcherComponent {
  private readonly translate = inject(TranslateService);
  private readonly languageService = inject(LanguageService);

  /** Toolbar: compact topbar. Settings: full-width row in Preferencias. */
  readonly layout = input<'toolbar' | 'settings'>('toolbar');

  @ViewChild(MatMenuTrigger) private readonly menuTrigger?: MatMenuTrigger;

  readonly supported = SUPPORTED_UI_LANGUAGES;

  readonly triggerDisplay = toSignal(
    this.translate.onLangChange.pipe(
      map((e) => e.lang),
      startWith(this.translate.currentLang),
      map((lang) => {
        const code = ((lang as SupportedUiLanguage) || 'es') in LANG_DISPLAY ? (lang as SupportedUiLanguage) : 'es';
        return { code, ...LANG_DISPLAY[code] };
      }),
    ),
    { initialValue: { code: 'es' as SupportedUiLanguage, ...LANG_DISPLAY.es } },
  );

  menuLabel(lang: SupportedUiLanguage): LangMeta {
    return LANG_DISPLAY[lang];
  }

  flagClasses(code: SupportedUiLanguage): string {
    return FLAG_MAP[code] ?? FLAG_MAP.es;
  }

  selectLang(code: SupportedUiLanguage): void {
    this.languageService.setLanguage(code);
    this.menuTrigger?.closeMenu();
  }
}
