import { Component, DestroyRef, NgZone, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet, Router, NavigationEnd, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from './core/auth/auth.service';
import { NotificacionesService } from './core/services/notificaciones.service';
import { AppAuthenticatedShellComponent } from './app-authenticated-shell/app-authenticated-shell.component';
import { LanguageSwitcherComponent } from './shared/language-switcher/language-switcher.component';
import { CookieBannerComponent } from './legal/cookie-banner/cookie-banner.component';
import { isLegalPath } from './legal/legal-paths';

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    AppAuthenticatedShellComponent,
    LanguageSwitcherComponent,
    CookieBannerComponent,
  ],
  template: `
    @if (auth.isAuthenticated() && !isLegalRoute()) {
      <app-authenticated-shell />
    }
    @if (!auth.isAuthenticated()) {
      <div class="app-public-shell">
        <header class="app-public-topbar">
          <span class="app-public-topbar__spacer"></span>
          <app-language-switcher />
        </header>
        <router-outlet></router-outlet>
      </div>
    }
    @if (auth.isAuthenticated() && isLegalRoute()) {
      <div class="app-legal-shell">
        <header class="app-legal-topbar">
          <a mat-stroked-button routerLink="/dashboard" class="app-legal-back">
            <mat-icon>arrow_back</mat-icon>
            Volver a la app
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
        padding: 8px 16px;
        background: color-mix(in srgb, var(--mat-sys-surface-container-low, #fafafa) 94%, transparent);
        backdrop-filter: blur(6px);
        border-bottom: 1px solid color-mix(in srgb, var(--mat-sys-outline-variant, #ccc) 55%, transparent);
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
      }
      .app-public-topbar__spacer {
        flex: 1;
      }
      .app-legal-main {
        flex: 1;
        min-width: 0;
      }
      .app-legal-back mat-icon {
        margin-right: 4px;
      }
    `,
  ],
})
export class AppComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notificaciones = inject(NotificacionesService);

  private readonly currentUrl = signal(this.router.url);
  readonly isLegalRoute = computed(() => isLegalPath(this.currentUrl()));

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
