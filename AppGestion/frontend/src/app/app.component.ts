import { Component, DestroyRef, NgZone, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/auth/auth.service';
import { NotificacionesService } from './core/services/notificaciones.service';
import { AppAuthenticatedShellComponent } from './app-authenticated-shell/app-authenticated-shell.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, AppAuthenticatedShellComponent],
  template: `
    @if (auth.isAuthenticated()) {
      <app-authenticated-shell />
    } @else {
      <router-outlet></router-outlet>
    }
  `,
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
