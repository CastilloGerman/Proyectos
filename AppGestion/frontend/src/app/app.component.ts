import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AsyncPipe } from '@angular/common';
import { AuthService } from './core/auth/auth.service';
import { SubscriptionService } from './core/services/subscription.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SearchBarComponent } from './shared/search-bar/search-bar.component';
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
    AsyncPipe,
    SearchBarComponent,
  ],
  template: `
    @if (auth.isAuthenticated()) {
      <mat-sidenav-container class="saas-layout">
        <mat-sidenav #drawer mode="side" [opened]="!sidebarCollapsed" class="saas-sidebar">
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
            <a mat-list-item routerLink="/presupuestos" routerLinkActive="active">
              <mat-icon matListItemIcon>description</mat-icon>
              <span matListItemTitle>Presupuestos</span>
            </a>
            <a mat-list-item routerLink="/facturas" routerLinkActive="active">
              <mat-icon matListItemIcon>receipt</mat-icon>
              <span matListItemTitle>Facturas</span>
            </a>
          </mat-nav-list>
        </mat-sidenav>
        <mat-sidenav-content class="saas-main">
          <header class="saas-topbar">
            <button mat-icon-button (click)="sidebarCollapsed = !sidebarCollapsed" matTooltip="{{ sidebarCollapsed ? 'Abrir menú' : 'Cerrar menú' }}">
              <mat-icon>menu</mat-icon>
            </button>
            <span class="spacer"></span>
            <app-search-bar></app-search-bar>
            <span class="user-email">{{ auth.user()?.email }}</span>
            <button mat-icon-button (click)="auth.logout()" matTooltip="Cerrar sesión">
              <mat-icon>logout</mat-icon>
            </button>
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
      background: var(--app-bg-page, #f8fafc);
    }

    .saas-sidebar {
      width: 260px;
      background: var(--app-bg-card) !important;
      border-right: 1px solid var(--app-border, rgba(0,0,0,0.06));
      box-shadow: var(--app-shadow-md, 0 4px 12px rgba(0,0,0,0.06));
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
      background: var(--app-bg-card);
      border-bottom: 1px solid var(--app-border, rgba(0,0,0,0.06));
      box-shadow: var(--app-shadow-sm, 0 1px 2px rgba(0,0,0,0.04));
    }

    .saas-topbar .spacer {
      flex: 1 1 auto;
    }

    .user-email {
      font-size: 0.875rem;
      color: var(--app-text-secondary, #64748b);
      margin-right: var(--app-space-sm, 8px);
    }

    .saas-content {
      flex: 1;
      padding: var(--app-space-lg, 24px);
      overflow: auto;
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
  `],
})
export class AppComponent implements OnInit {
  sidebarCollapsed = false;

  constructor(
    public auth: AuthService,
    private subscriptionService: SubscriptionService,
    private snackBar: MatSnackBar,
    private http: HttpClient
  ) {}

  protected readonly environment = environment;

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.auth.refreshUser().subscribe();
    }
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
