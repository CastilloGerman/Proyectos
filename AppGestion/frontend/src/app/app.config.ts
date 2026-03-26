import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    /* eventCoalescing en true puede retrasar/meclar CD con Material/tablas; false evita filas que solo pintan tras clics */
    provideZoneChangeDetection({ eventCoalescing: false }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    /* Async puede dejar chips/botones sin pintar hasta interacción; sync evita estados de animación colgados */
    provideAnimations(),
    importProvidersFrom(MatSnackBarModule),
  ],
};
