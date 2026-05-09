import { Component, inject, ViewChild } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { map, startWith } from 'rxjs/operators';
import { LanguageService, SUPPORTED_UI_LANGUAGES, SupportedUiLanguage } from '../../core/i18n/language.service';

const LANG_DISPLAY: Record<SupportedUiLanguage, { flag: string; short: string; native: string }> = {
  es: { flag: '🇪🇸', short: 'ES', native: 'Español' },
  en: { flag: '🇬🇧', short: 'EN', native: 'English' },
  fr: { flag: '🇫🇷', short: 'FR', native: 'Français' },
  ro: { flag: '🇷🇴', short: 'RO', native: 'Română' },
  uk: { flag: '🇺🇦', short: 'UK', native: 'Українська' },
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

  @ViewChild(MatMenuTrigger) private readonly menuTrigger?: MatMenuTrigger;

  readonly supported = SUPPORTED_UI_LANGUAGES;

  readonly triggerDisplay = toSignal(
    this.translate.onLangChange.pipe(
      map((e) => e.lang),
      startWith(this.translate.currentLang),
      map((lang) => LANG_DISPLAY[(lang as SupportedUiLanguage) ?? 'es'] ?? LANG_DISPLAY.es),
    ),
    { initialValue: LANG_DISPLAY[this.translate.currentLang as SupportedUiLanguage] ?? LANG_DISPLAY.es },
  );

  menuLabel(lang: SupportedUiLanguage): { flag: string; short: string; native: string } {
    return LANG_DISPLAY[lang];
  }

  selectLang(code: SupportedUiLanguage): void {
    this.languageService.setLanguage(code);
    this.menuTrigger?.closeMenu();
  }
}
