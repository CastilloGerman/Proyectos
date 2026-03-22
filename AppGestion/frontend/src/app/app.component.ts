import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { map } from 'rxjs/operators';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
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
import { AsyncPipe } from '@angular/common';
import { AuthService } from './core/auth/auth.service';
import { SubscriptionService } from './core/services/subscription.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { InvitarUsuarioDialogComponent } from './shared/invitar-usuario-dialog/invitar-usuario-dialog.component';
import { SearchBarComponent } from './shared/search-bar/search-bar.component';
import { UserDropdownComponent } from './shared/user-dropdown/user-dropdown.component';
import { NotificacionesService, NotificacionDto } from './core/services/notificaciones.service';
import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  standalone: true,
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
    AsyncPipe,
    SearchBarComponent,
    UserDropdownComponent,
  ],
  template: `
    @if (auth.isAuthenticated()) {
      <mat-sidenav-container class="saas-layout">
        <mat-sidenav
          #drawer
          class="saas-sidebar"
          [mode]="isMobileLayout() ? 'over' : 'side'"
          [opened]="isMobileLayout() ? isSidebarOpen : !sidebarCollapsed"
          [fixedInViewport]="isMobileLayout()"
          (openedChange)="onSidenavOpenedChange($event)"
        >
          <div class="sidebar-header">
            <span class="brand-logo">
              <img src="assets/noemi-logo.png" alt="Noemí" class="brand-logo-img" />
            </span>
          </div>
          <mat-nav-list class="sidebar-nav">
            <a mat-list-item routerLink="/dashboard" routerLinkActive="active">
              <mat-icon matListItemIcon>dashboard</mat-icon>
              <span matListItemTitle>Dashboard</span>
            </a>
            <a mat-list-item routerLink="/materiales" routerLinkActive="active">
              <mat-icon matListItemIcon>inventory_2</mat-icon>
              <span matListItemTitle>Materiales</span>
            </a>
            <a mat-list-item routerLink="/clientes" routerLinkActive="active">
              <mat-icon matListItemIcon>people</mat-icon>
              <span matListItemTitle>Clientes</span>
            </a>
            <a mat-list-item routerLink="/presupuestos" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
              <mat-icon matListItemIcon>description</mat-icon>
              <span matListItemTitle>Presupuestos</span>
            </a>
            @if (auth.canMutate()) {
            <a mat-list-item routerLink="/presupuestos/rapido" routerLinkActive="active">
              <mat-icon matListItemIcon>bolt</mat-icon>
              <span matListItemTitle>Presup. rápido</span>
            </a>
            }
            <a mat-list-item routerLink="/facturas" routerLinkActive="active">
              <mat-icon matListItemIcon>receipt</mat-icon>
              <span matListItemTitle>Facturas</span>
            </a>
          </mat-nav-list>
        </mat-sidenav>
        <mat-sidenav-content class="saas-main">
          <header class="saas-topbar">
            <button
              mat-icon-button
              type="button"
              (click)="toggleSidenavMenu()"
              [matTooltip]="sidenavMenuTooltip()"
              aria-label="Abrir o cerrar menú lateral"
            >
              <mat-icon>menu</mat-icon>
            </button>
            <app-search-bar></app-search-bar>
            <span class="spacer"></span>
            @if (auth.canMutate()) {
            <button mat-stroked-button (click)="openInvitar()" matTooltip="Enviar enlace de referido por correo">
              Referir
            </button>
            }
            <button
              type="button"
              mat-icon-button
              #notifMenuTrigger="matMenuTrigger"
              [matMenuTriggerFor]="notifMenu"
              (menuOpened)="onNotifMenuOpened()"
              matTooltip="Notificaciones"
              [matBadge]="notifBadge()"
              [matBadgeHidden]="notificaciones.unreadCount() === 0"
              matBadgeColor="warn"
              matBadgeSize="small"
              aria-label="Abrir notificaciones"
              aria-haspopup="menu"
            >
              <mat-icon>notifications</mat-icon>
            </button>
            <mat-menu #notifMenu="matMenu" class="notif-bell-menu" xPosition="before">
              <div class="notif-bell-panel" (click)="$event.stopPropagation()">
                <div class="notif-bell-head">Notificaciones</div>
                @if (notifPreviewLoading()) {
                  <div class="notif-bell-center">
                    <mat-progress-spinner diameter="32" mode="indeterminate" />
                  </div>
                } @else if (notifPreviewError()) {
                  <div class="notif-bell-empty notif-bell-empty--error">
                    <mat-icon>error_outline</mat-icon>
                    <p>No se pudieron cargar las notificaciones.</p>
                  </div>
                } @else if (notifPreviewItems().length === 0) {
                  <div class="notif-bell-empty">
                    <mat-icon>notifications_off</mat-icon>
                    <p><strong>No hay notificaciones nuevas</strong></p>
                    <p class="notif-bell-empty-hint">Cuando tengas avisos sin leer, aparecerán aquí.</p>
                  </div>
                } @else {
                  <div class="notif-bell-list">
                    @for (n of notifPreviewItems(); track n.id) {
                      <button type="button" class="notif-bell-item" (click)="abrirNotificacionPreview(n, notifMenuTrigger)">
                        <mat-icon [class.notif-bell-item__icon--warn]="n.severidad === 'WARNING' || n.severidad === 'ERROR'">
                          {{ iconoNotificacion(n) }}
                        </mat-icon>
                        <div class="notif-bell-item__text">
                          <span class="notif-bell-item__title">{{ n.titulo }}</span>
                          @if (n.resumen) {
                            <span class="notif-bell-item__sum">{{ n.resumen }}</span>
                          }
                        </div>
                      </button>
                    }
                  </div>
                }
              </div>
              <mat-divider />
              <button mat-menu-item type="button" routerLink="/cuenta/notificaciones" (click)="notifMenuTrigger.closeMenu()">
                <mat-icon>list</mat-icon>
                <span>Ver todas las notificaciones</span>
              </button>
            </mat-menu>
            <app-user-dropdown />
          </header>
          @if (!auth.canWrite()) {
            <div class="banner-readonly">
              <mat-icon>lock</mat-icon>
              <span>Modo solo lectura. Activa tu suscripción para crear o editar.</span>
              <button mat-stroked-button color="warn" (click)="activarSuscripcion()">
                Activar suscripción
              </button>
              @if (!environment.production) {
                <button mat-stroked-button (click)="grantPremiumDev()" matTooltip="Solo desarrollo: marcar usuario como premium sin Stripe">
                  Activar premium (dev)
                </button>
              }
            </div>
          }
          @if (auth.canWrite() && auth.user()?.subscriptionStatus === 'TRIAL_ACTIVE' && daysLeftInTrial() <= 7 && daysLeftInTrial() >= 0) {
            <div class="banner-trial">
              <mat-icon>schedule</mat-icon>
              <span>Tu prueba termina en {{ daysLeftInTrial() }} días.</span>
              <button mat-stroked-button (click)="activarSuscripcion()">
                Activar suscripción
              </button>
            </div>
          }
          <main class="saas-content">
            <router-outlet></router-outlet>
          </main>
        </mat-sidenav-content>
      </mat-sidenav-container>
    } @else {
      <router-outlet></router-outlet>
    }
  `,
  styles: [`
    .saas-layout {
      height: 100vh;
      height: 100dvh;
      max-width: 100%;
      background: var(--app-bg-page, #f8fafc);
      overflow-x: hidden;
    }

    .saas-sidebar {
      width: min(260px, 85vw);
      max-width: 100%;
      background: var(--app-bg-card) !important;
      border-right: 1px solid var(--app-border, rgba(0,0,0,0.06));
      box-shadow: var(--app-shadow-md, 0 4px 12px rgba(0,0,0,0.06));
      overflow-x: hidden;
    }

    .saas-sidebar .sidebar-header {
      padding: var(--app-space-xs, 4px) var(--app-space-sm, 8px);
      min-height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-bottom: 1px solid var(--app-border, rgba(0,0,0,0.06));
    }

    .brand-logo {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;
      min-height: 100px;
    }

    .brand-logo-img {
      height: 100px;
      min-height: 100px;
      width: auto;
      max-width: 100%;
      object-fit: contain;
      object-position: center;
      display: block;
    }

    .sidebar-nav {
      padding: var(--app-space-sm, 8px) 0;
    }

    .sidebar-nav a {
      border-radius: var(--app-radius-md, 12px);
      margin: 0 var(--app-space-sm, 8px);
      transition: background var(--app-transition, 0.2s ease);
    }

    .sidebar-nav a:hover {
      background: rgba(30, 58, 138, 0.06);
    }

    .sidebar-nav a.active {
      background: rgba(30, 58, 138, 0.1);
      color: #1e3a8a;
      font-weight: 500;
    }

    .saas-main {
      display: flex;
      flex-direction: column;
      min-width: 0;
      max-width: 100%;
      overflow-x: hidden;
      box-sizing: border-box;
      background: var(--app-bg-page, #f8fafc);
    }

    .saas-topbar {
      position: sticky;
      top: 0;
      z-index: 100;
      display: flex;
      align-items: center;
      gap: var(--app-space-sm, 8px);
      padding: 0 var(--app-space-md, 16px);
      min-height: 56px;
      min-width: 0;
      max-width: 100%;
      box-sizing: border-box;
      background: var(--app-bg-card);
      border-bottom: 1px solid var(--app-border, rgba(0,0,0,0.06));
      box-shadow: var(--app-shadow-sm, 0 1px 2px rgba(0,0,0,0.04));
    }

    .saas-topbar app-search-bar {
      flex: 1 1 auto;
      min-width: 0;
      display: block;
    }

    .saas-topbar .spacer {
      flex: 1 1 auto;
      min-width: 0;
    }

    .saas-content {
      flex: 1;
      min-width: 0;
      max-width: 100%;
      padding: var(--app-space-lg, 24px);
      overflow-x: hidden;
      overflow-y: auto;
      box-sizing: border-box;
    }

    @media (max-width: 768px) {
      .saas-topbar {
        padding: 0 var(--app-space-sm, 8px);
        gap: 4px;
      }

      .saas-content {
        padding: var(--app-space-md, 16px) var(--app-space-sm, 8px);
      }

      .banner-readonly,
      .banner-trial {
        margin-left: var(--app-space-sm, 8px);
        margin-right: var(--app-space-sm, 8px);
        max-width: calc(100% - 16px);
        box-sizing: border-box;
        flex-wrap: wrap;
      }

      .saas-topbar .mat-mdc-outlined-button {
        padding: 0 8px;
        font-size: 12px;
      }
    }

    .banner-readonly {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 24px;
      background: #fef2f2;
      color: #b91c1c;
      font-size: 14px;
      border-radius: var(--app-radius-md, 12px);
      margin: var(--app-space-md, 16px) var(--app-space-lg, 24px);
    }

    .banner-readonly mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .banner-trial {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 24px;
      background: #fff7ed;
      color: #c2410c;
      font-size: 14px;
      border-radius: var(--app-radius-md, 12px);
      margin: var(--app-space-md, 16px) var(--app-space-lg, 24px);
    }

    .banner-trial mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    ::ng-deep .notif-bell-menu {
      max-width: min(380px, 96vw);
    }

    .notif-bell-panel {
      max-height: 340px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      min-width: 280px;
    }

    .notif-bell-head {
      padding: 12px 16px 8px;
      font-weight: 600;
      font-size: 14px;
      color: var(--app-text-primary, #0f172a);
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      flex-shrink: 0;
    }

    .notif-bell-center {
      display: flex;
      justify-content: center;
      padding: 28px 16px;
    }

    .notif-bell-empty {
      padding: 20px 18px 16px;
      text-align: center;
      color: var(--app-text-secondary, #64748b);
      font-size: 13px;
      line-height: 1.45;
    }

    .notif-bell-empty mat-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      margin-bottom: 8px;
      opacity: 0.45;
      color: var(--mat-sys-primary, #1e40af);
    }

    .notif-bell-empty--error mat-icon {
      color: #b91c1c;
      opacity: 0.85;
    }

    .notif-bell-empty-hint {
      margin: 8px 0 0;
      font-size: 12px;
      opacity: 0.9;
    }

    .notif-bell-list {
      overflow-y: auto;
      max-height: 260px;
    }

    .notif-bell-item {
      display: flex;
      gap: 10px;
      width: 100%;
      text-align: left;
      padding: 10px 14px;
      border: none;
      background: transparent;
      cursor: pointer;
      font: inherit;
      color: inherit;
      align-items: flex-start;
      border-bottom: 1px solid rgba(0, 0, 0, 0.04);
    }

    .notif-bell-item:hover {
      background: rgba(30, 64, 175, 0.06);
    }

    .notif-bell-item mat-icon {
      flex-shrink: 0;
      font-size: 20px;
      width: 20px;
      height: 20px;
      margin-top: 2px;
      color: var(--mat-sys-primary, #1e40af);
    }

    .notif-bell-item__icon--warn {
      color: #c2410c !important;
    }

    .notif-bell-item__text {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
    }

    .notif-bell-item__title {
      font-size: 13px;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
      line-height: 1.3;
    }

    .notif-bell-item__sum {
      font-size: 12px;
      color: var(--app-text-secondary, #64748b);
      line-height: 1.35;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  `],
})
export class AppComponent implements OnInit {
  /** Escritorio: false = barra visible (comportamiento actual). */
  sidebarCollapsed = false;
  /** Móvil (overlay): true = drawer abierto. */
  isSidebarOpen = false;

  private readonly breakpointObserver = inject(BreakpointObserver);
  readonly isMobileLayout = toSignal(
    this.breakpointObserver.observe('(max-width: 768px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  readonly notificaciones = inject(NotificacionesService);
  private readonly router = inject(Router);

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

  constructor(
    public auth: AuthService,
    private subscriptionService: SubscriptionService,
    private snackBar: MatSnackBar,
    private http: HttpClient,
    private dialog: MatDialog
  ) {}

  protected readonly environment = environment;

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.auth.refreshUser().subscribe();
      this.notificaciones.refreshUnreadCount();
    }
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(() => {
      if (this.auth.isAuthenticated()) {
        this.notificaciones.refreshUnreadCount();
      }
      if (this.isMobileLayout()) {
        this.isSidebarOpen = false;
      }
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

  sidenavMenuTooltip(): string {
    if (this.isMobileLayout()) {
      return this.isSidebarOpen ? 'Cerrar menú' : 'Abrir menú';
    }
    return this.sidebarCollapsed ? 'Abrir menú' : 'Cerrar menú';
  }

  daysLeftInTrial(): number {
    const end = this.auth.user()?.trialEndDate;
    if (!end) return 99;
    const endDate = new Date(end);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    endDate.setHours(0, 0, 0, 0);
    const diff = Math.ceil((endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    return diff;
  }

  openInvitar(): void {
    const ref = this.dialog.open(InvitarUsuarioDialogComponent, { width: 'min(480px, 96vw)', maxWidth: '96vw' });
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.snackBar.open('Enlace de referido enviado. Revisa el correo (o los logs del servidor si no hay SMTP).', 'Cerrar', {
          duration: 5000,
        });
      }
    });
  }

  activarSuscripcion(): void {
    this.subscriptionService.createCheckoutSession().subscribe({
      next: (res) => {
        if (res.checkoutUrl) {
          window.location.href = res.checkoutUrl;
        }
      },
      error: (err) => {
        this.snackBar.open(err.error?.error || 'Error al crear sesión de pago', 'Cerrar', { duration: 4000 });
      },
    });
  }

  /** Solo desarrollo: marca al usuario actual como premium (ACTIVE) sin Stripe. */
  grantPremiumDev(): void {
    this.http.post<{ ok: boolean; message: string }>(`${environment.apiUrl}/dev/grant-premium`, {}).subscribe({
      next: () => {
        this.auth.refreshUser().subscribe(() => {
          this.snackBar.open('Premium activado. La app ya tiene permisos de escritura.', 'Cerrar', { duration: 4000 });
        });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || err.message || 'Error al activar premium (¿API con perfil local?)', 'Cerrar', { duration: 5000 });
      },
    });
  }
}
