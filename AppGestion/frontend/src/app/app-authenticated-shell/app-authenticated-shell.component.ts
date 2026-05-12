import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule, MatMenuTrigger } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../core/auth/auth.service';
import { SubscriptionService } from '../core/services/subscription.service';
import { InvitarUsuarioDialogComponent } from '../shared/invitar-usuario-dialog/invitar-usuario-dialog.component';
import { SearchBarComponent } from '../shared/search-bar/search-bar.component';
import { UserDropdownComponent } from '../shared/user-dropdown/user-dropdown.component';
import { NotificacionesService, NotificacionDto } from '../core/services/notificaciones.service';
import { environment } from '../../environments/environment';
import { ThemeService } from '../core/theme/theme.service';
import { DevApiService } from '../core/services/dev-api.service';
import { daysFromTodayToDateEnd } from '../shared/utils/trial-days.util';
import { TRIAL_BANNER_WARNING_DAYS, TRIAL_DAYS_LEFT_FALLBACK } from '../app-layout.constants';
import { messageFromHttpError } from '../shared/utils/http-error-message.util';

@Component({
  selector: 'app-authenticated-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatTooltipModule,
    MatBadgeModule,
    MatMenuModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    SearchBarComponent,
    UserDropdownComponent,
    TranslateModule,
  ],
  templateUrl: './app-authenticated-shell.component.html',
  styleUrl: './app-authenticated-shell.component.scss',
})
export class AppAuthenticatedShellComponent implements OnInit {
  /** Fuerza la creación de ThemeService al montar el layout autenticado. */
  private readonly _theme = inject(ThemeService);

  sidebarCollapsed = false;
  isSidebarOpen = false;

  private readonly breakpointObserver = inject(BreakpointObserver);
  readonly isMobileLayout = toSignal(
    this.breakpointObserver.observe('(max-width: 768px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  readonly auth = inject(AuthService);
  readonly notificaciones = inject(NotificacionesService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly devApi = inject(DevApiService);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly trialBannerWarningDays = TRIAL_BANNER_WARNING_DAYS;
  protected readonly environment = environment;

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        if (this.isMobileLayout()) {
          this.isSidebarOpen = false;
        }
      });
  }

  readonly notifBadge = computed(() => {
    const n = this.notificaciones.unreadCount();
    if (n <= 0) {
      return '';
    }
    return n > 99 ? '99+' : String(n);
  });

  readonly notifPreviewItems = signal<NotificacionDto[]>([]);
  readonly notifPreviewLoading = signal(false);
  readonly notifPreviewError = signal(false);
  readonly notifMarcarTodasLoading = signal(false);

  onNotifMenuOpened(): void {
    this.notifPreviewError.set(false);
    this.notifPreviewLoading.set(true);
    this.notificaciones.refreshUnreadCount();
    this.notificaciones.list(0, 10, 'no_leidas').subscribe({
      next: (p) => {
        this.notifPreviewItems.set(p.content ?? []);
        this.notifPreviewLoading.set(false);
      },
      error: () => {
        this.notifPreviewItems.set([]);
        this.notifPreviewError.set(true);
        this.notifPreviewLoading.set(false);
      },
    });
  }

  iconoNotificacion(n: NotificacionDto): string {
    switch (n.severidad) {
      case 'ERROR':
        return 'error';
      case 'WARNING':
        return 'warning';
      default:
        return 'info';
    }
  }

  abrirNotificacionPreview(n: NotificacionDto, trigger: MatMenuTrigger): void {
    trigger.closeMenu();
    if (!n.leida) {
      this.notificaciones.markRead(n.id).subscribe({ error: () => {} });
      this.notifPreviewItems.update((list) => list.map((x) => (x.id === n.id ? { ...x, leida: true } : x)));
    }
    const path = n.actionPath?.trim();
    if (path && path.startsWith('/') && !path.startsWith('//')) {
      void this.router.navigateByUrl(path);
    }
  }

  /** Marcar todas las no leídas desde la campana: limpia contador y vista previa. */
  marcarTodasLeidasBell(ev: MouseEvent): void {
    ev.stopPropagation();
    if (this.notificaciones.unreadCount() === 0 || this.notifMarcarTodasLoading()) return;
    this.notifMarcarTodasLoading.set(true);
    this.notificaciones.markAllRead().subscribe({
      next: () => {
        this.notifMarcarTodasLoading.set(false);
        this.notifPreviewItems.set([]);
        this.snackBar.open(
          this.translate.instant('shell.snackbarNotifMarkedRead'),
          this.translate.instant('common.close'),
          { duration: 2500 },
        );
      },
      error: () => {
        this.notifMarcarTodasLoading.set(false);
        this.snackBar.open(
          this.translate.instant('shell.snackbarNotifMarkFailed'),
          this.translate.instant('common.close'),
          { duration: 4000 },
        );
      },
    });
  }

  toggleSidenavMenu(): void {
    if (this.isMobileLayout()) {
      this.isSidebarOpen = !this.isSidebarOpen;
    } else {
      this.sidebarCollapsed = !this.sidebarCollapsed;
    }
  }

  onSidenavOpenedChange(opened: boolean): void {
    if (this.isMobileLayout()) {
      this.isSidebarOpen = opened;
    } else {
      this.sidebarCollapsed = !opened;
    }
  }

  sidenavMenuTooltipKey(): string {
    if (this.isMobileLayout()) {
      return this.isSidebarOpen ? 'shell.closeMenu' : 'shell.openMenu';
    }
    return this.sidebarCollapsed ? 'shell.openMenu' : 'shell.closeMenu';
  }

  daysLeftInTrial(): number {
    const end = this.auth.user()?.trialEndDate;
    if (!end) return TRIAL_DAYS_LEFT_FALLBACK;
    const d = daysFromTodayToDateEnd(end);
    return d ?? TRIAL_DAYS_LEFT_FALLBACK;
  }

  private snackHttpPresets() {
    return {
      offline: this.translate.instant('shell.snackbarOffline'),
      server: this.translate.instant('shell.snackbarServerError'),
    };
  }

  openInvitar(): void {
    const ref = this.dialog.open(InvitarUsuarioDialogComponent, { width: 'min(480px, 96vw)', maxWidth: '96vw' });
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.snackBar.open(this.translate.instant('shell.snackbarReferSent'), this.translate.instant('common.close'), {
          duration: 5000,
        });
      }
    });
  }

  activarSuscripcion(): void {
    this.subscriptionService.createCheckoutSession().subscribe({
      next: (res) => {
        const url = res.checkoutUrl?.trim();
        if (url) {
          window.location.href = url;
        } else {
          this.snackBar.open(
            this.translate.instant('shell.snackbarNoCheckoutUrl'),
            this.translate.instant('common.close'),
            { duration: 4000 },
          );
        }
      },
      error: (err: unknown) => {
        const msg = messageFromHttpError(err, this.translate.instant('shell.snackbarPaymentErrorFallback'), this.snackHttpPresets());
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 6500 });
      },
    });
  }

  grantPremiumDev(): void {
    this.devApi.grantPremium().subscribe({
      next: () => {
        this.auth.refreshUser().subscribe(() => {
          this.snackBar.open(
            this.translate.instant('shell.snackbarPremiumActivated'),
            this.translate.instant('common.close'),
            { duration: 4000 },
          );
        });
      },
      error: (err) => {
        const msg = messageFromHttpError(
          err,
          this.translate.instant('shell.snackbarGrantPremiumFallback'),
          this.snackHttpPresets(),
        );
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }
}
