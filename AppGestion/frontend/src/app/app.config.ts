import { APP_INITIALIZER, ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { provideTranslateLoader, TranslateModule } from '@ngx-translate/core';
import { TranslateHttpLoader, TRANSLATE_HTTP_LOADER_CONFIG } from '@ngx-translate/http-loader';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { LanguageService } from './core/i18n/language.service';

function initLanguage(languageService: LanguageService): () => Promise<void> {
  return () => languageService.init();
}

export const appConfig: ApplicationConfig = {
  providers: [
    /* eventCoalescing en true puede retrasar/meclar CD con Material/tablas; false evita filas que solo pintan tras clics */
    provideZoneChangeDetection({ eventCoalescing: false }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    /* Async puede dejar chips/botones sin pintar hasta interacción; sync evita estados de animación colgados */
    provideAnimations(),
    importProvidersFrom(MatSnackBarModule),
    { provide: TRANSLATE_HTTP_LOADER_CONFIG, useValue: { prefix: 'assets/i18n/', suffix: '.json' } },
    importProvidersFrom(
      TranslateModule.forRoot({
        fallbackLang: 'es',
        loader: provideTranslateLoader(TranslateHttpLoader),
      }),
    ),
    {
      provide: APP_INITIALIZER,
      useFactory: initLanguage,
      deps: [LanguageService],
      multi: true,
    },
  ],
};
