import { Component, DestroyRef, NgZone, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/auth/auth.service';
import { NotificacionesService } from './core/services/notificaciones.service';
import { AppAuthenticatedShellComponent } from './app-authenticated-shell/app-authenticated-shell.component';
import { LanguageSwitcherComponent } from './shared/language-switcher/language-switcher.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppAuthenticatedShellComponent, LanguageSwitcherComponent],
  template: `
    @if (auth.isAuthenticated()) {
      <app-authenticated-shell />
    } @else {
      <div class="app-public-shell">
        <header class="app-public-topbar">
          <span class="app-public-topbar__spacer"></span>
          <app-language-switcher />
        </header>
        <router-outlet></router-outlet>
      </div>
    }
  `,
  styles: [
    `
      .app-public-shell {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
      }
      .app-public-topbar {
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
    `,
  ],
})
export class AppComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ngZone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notificaciones = inject(NotificacionesService);

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
      .subscribe(() => {
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
