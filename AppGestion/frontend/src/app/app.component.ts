import { Component, DestroyRef, NgZone, OnInit, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet, Router, NavigationEnd, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';
import { inject as injectAnalytics } from '@vercel/analytics';
import { AuthService } from './core/auth/auth.service';
import { NotificacionesService } from './core/services/notificaciones.service';
import { CookieConsentService } from './core/consent/cookie-consent.service';
import { TranslateModule } from '@ngx-translate/core';
import { AppAuthenticatedShellComponent } from './app-authenticated-shell/app-authenticated-shell.component';
import { LanguageSwitcherComponent } from './shared/language-switcher/language-switcher.component';
import { CookieBannerComponent } from './legal/cookie-banner/cookie-banner.component';
import { ThemeService } from './core/theme/theme.service';
import { isLegalPath } from './legal/legal-paths';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    TranslateModule,
    AppAuthenticatedShellComponent,
    LanguageSwitcherComponent,
    CookieBannerComponent,
  ],
  template: `
    @if (auth.isAuthenticated() && !isLegalRoute() && !isPublicBudgetRoute()) {
      <app-authenticated-shell />
    }
    @if (!auth.isAuthenticated() || isPublicBudgetRoute()) {
      <div class="app-public-shell">
        @if (!isPublicBudgetRoute()) {
          <header class="app-public-topbar" [class.app-public-topbar--login]="isLoginRoute()">
            @if (isLegalRoute()) {
            <a routerLink="/login" class="app-topbar-logo" aria-label="Volver al inicio de sesión">
              <img src="assets/noemi-logo.png" alt="Noemi" />
            </a>
            }
            <span class="app-public-topbar__spacer"></span>
            <div class="app-public-topbar__tools">
              @if (isLoginRoute()) {
                <button
                  type="button"
                  class="app-theme-toggle"
                  (click)="theme.toggle()"
                  [attr.aria-pressed]="theme.isDark()"
                  [attr.aria-label]="(theme.isDark() ? 'auth.login.themeToLight' : 'auth.login.themeToDark') | translate"
                >
                  <svg class="app-theme-toggle__icon" viewBox="0 0 24 24" aria-hidden="true">
                    @if (theme.isDark()) {
                      <path fill="currentColor" d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0a.996.996 0 0 0 0-1.41l-1.06-1.06zm1.06-10.96a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/>
                    } @else {
                      <path fill="currentColor" d="M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z"/>
                    }
                  </svg>
                </button>
              }
              <app-language-switcher />
            </div>
          </header>
        }
        <router-outlet></router-outlet>
      </div>
    }
    @if (auth.isAuthenticated() && isLegalRoute()) {
      <div class="app-legal-shell">
        <header class="app-legal-topbar">
          <a routerLink="/dashboard" class="app-topbar-logo" aria-label="Volver a la aplicación">
            <img src="assets/noemi-logo.png" alt="Noemi" />
          </a>
          <span class="app-public-topbar__spacer"></span>
          <app-language-switcher />
        </header>
        <main class="app-legal-main">
          <router-outlet></router-outlet>
        </main>
      </div>
    }
    <app-cookie-banner />
  `,
  styles: [
    `
      .app-public-shell,
      .app-legal-shell {
        min-height: 100vh;
        min-height: 100dvh;
        display: flex;
        flex-direction: column;
        background: var(--app-bg-page, #f8fafc);
      }
      .app-public-topbar,
      .app-legal-topbar {
        position: sticky;
        top: 0;
        z-index: 20;
        display: flex;
        align-items: center;
        gap: 12px;
        min-height: 56px;
        padding: 4px 16px;
        background: color-mix(in srgb, var(--mat-sys-surface-container-low, #fafafa) 94%, transparent);
        backdrop-filter: blur(6px);
        border-bottom: 1px solid color-mix(in srgb, var(--mat-sys-outline-variant, #ccc) 55%, transparent);
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
      }
      .app-public-topbar__spacer {
        flex: 1;
      }
      .app-public-topbar__tools {
        display: flex;
        align-items: center;
        gap: 8px;
      }
      /* En login la barra sigue el fondo de la página, no una franja clara aparte. */
      .app-public-topbar--login {
        background: #eceef4;
        border-bottom-color: transparent;
        box-shadow: none;
      }
      .app-theme-toggle {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 40px;
        height: 40px;
        padding: 0;
        border-radius: 10px;
        border: 1px solid rgba(15, 23, 42, 0.16);
        background: rgba(255, 255, 255, 0.55);
        color: #334155;
        cursor: pointer;
        transition: background-color 160ms ease, border-color 160ms ease, color 160ms ease;
      }
      .app-theme-toggle:hover {
        background: #fff;
        border-color: rgba(15, 23, 42, 0.28);
      }
      .app-theme-toggle__icon {
        width: 22px;
        height: 22px;
        display: block;
      }
      :host-context(html.app-dark-theme) .app-public-topbar--login {
        background: #0c1220;
        border-bottom-color: transparent;
        box-shadow: none;
      }
      :host-context(html.app-dark-theme) .app-public-topbar--login .app-theme-toggle {
        color: #e2e8f0;
        border-color: rgba(226, 232, 240, 0.28);
        background: rgba(255, 255, 255, 0.04);
      }
      :host-context(html.app-dark-theme) .app-public-topbar--login .app-theme-toggle:hover {
        background: rgba(255, 255, 255, 0.1);
        border-color: rgba(226, 232, 240, 0.45);
      }
      :host-context(html.app-dark-theme) .app-public-topbar--login ::ng-deep .lang-switcher__trigger.mat-mdc-outlined-button {
        --mdc-outlined-button-label-text-color: #e2e8f0;
        --mdc-outlined-button-outline-color: rgba(226, 232, 240, 0.28);
        --mat-outlined-button-label-text-color: #e2e8f0;
        --mat-outlined-button-outline-color: rgba(226, 232, 240, 0.28);
        color: #e2e8f0;
        background: rgba(255, 255, 255, 0.04);
      }
      :host-context(html.app-dark-theme) .app-public-topbar--login ::ng-deep .lang-switcher__trigger.mat-mdc-outlined-button:hover {
        background: rgba(255, 255, 255, 0.1);
      }
      .app-legal-main {
        flex: 1;
        min-width: 0;
      }
      .app-topbar-logo {
        display: inline-flex;
        align-items: center;
        align-self: stretch;
        flex-shrink: 0;
        line-height: 0;
        padding: 2px 0;
        border-radius: 8px;
        transition: opacity 0.2s ease;
      }
      .app-topbar-logo:hover {
        opacity: 0.85;
      }
      .app-topbar-logo img {
        height: 52px;
        width: auto;
        max-height: 100%;
        object-fit: contain;
        display: block;
      }
    `,
  ],
})
export class AppComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notificaciones = inject(NotificacionesService);

  private readonly currentUrl = signal(this.router.url);
  readonly isLegalRoute = computed(() => isLegalPath(this.currentUrl()));
  readonly isPublicBudgetRoute = computed(() => this.currentUrl().startsWith('/p/'));
  readonly isLoginRoute = computed(() => this.currentUrl().split(/[?#]/)[0] === '/login');

  private readonly cookieConsent = inject(CookieConsentService);
  private analyticsLoaded = false;

  constructor() {
    // SEC-32: la analítica de terceros (Vercel Web Analytics) solo se carga tras un "Aceptar" explícito.
    effect(() => {
      if (this.cookieConsent.status() === 'accepted' && !this.analyticsLoaded && typeof window !== 'undefined') {
        this.analyticsLoaded = true;
        injectAnalytics();
      }
    });
  }

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.auth.refreshUser().subscribe();
      this.notificaciones.refreshUnreadCount();
    }
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((e) => {
        this.currentUrl.set(e.urlAfterRedirects);
        if (this.auth.isAuthenticated()) {
          this.notificaciones.refreshUnreadCount();
          this.ngZone.runOutsideAngular(() => {
            requestAnimationFrame(() =>
              requestAnimationFrame(() => this.ngZone.run(() => window.dispatchEvent(new Event('resize')))),
            );
          });
        }
      });
  }
}
