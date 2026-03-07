import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
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
      <mat-sidenav-container class="sidenav-container">
        <mat-sidenav #drawer mode="side" opened class="sidenav">
          <mat-toolbar color="primary">
            <span class="brand-logo">
              <img src="assets/noemi-logo.png" alt="Noemí" class="brand-logo-img" />
            </span>
          </mat-toolbar>
          <mat-nav-list>
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
        <mat-sidenav-content>
          @if (!auth.canWrite()) {
            <div class="banner-readonly">
              <mat-icon>lock</mat-icon>
              <span>Modo solo lectura. Activa tu suscripción para crear o editar.</span>
              <button mat-stroked-button color="warn" (click)="activarSuscripcion()">
                Activar suscripción
              </button>
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
          <mat-toolbar color="primary" class="header-toolbar">
            <span class="spacer"></span>
            <app-search-bar></app-search-bar>
            <span class="user-email">{{ auth.user()?.email }}</span>
            <button mat-icon-button (click)="auth.logout()" matTooltip="Cerrar sesión">
              <mat-icon>logout</mat-icon>
            </button>
          </mat-toolbar>
          <main class="content">
            <router-outlet></router-outlet>
          </main>
        </mat-sidenav-content>
      </mat-sidenav-container>
    } @else {
      <router-outlet></router-outlet>
    }
  `,
  styles: [`
    .sidenav-container {
      height: 100vh;
    }

    .sidenav {
      width: 240px;
    }

    .sidenav .mat-toolbar {
      background: inherit;
    }

    .brand-logo {
      display: inline-flex;
      align-items: center;
    }

    .brand-logo-img {
      height: 52px;
      width: auto;
      object-fit: contain;
      display: block;
    }

    .header-toolbar {
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .spacer {
      flex: 1 1 auto;
    }

    .user-email {
      font-size: 14px;
      margin-right: 8px;
    }

    mat-sidenav-content {
      background: linear-gradient(160deg, #e8ecf4 0%, #e2e7f0 50%, #dce1eb 100%);
    }

    .content {
      padding: 24px;
      min-height: calc(100vh - 64px);
      background: rgba(255, 255, 255, 0.7);
      border-radius: 24px 0 0 0;
      box-shadow: 0 0 40px rgba(30, 58, 138, 0.04);
      transition: box-shadow 0.3s ease;
    }

    .active {
      background-color: rgba(0, 0, 0, 0.04);
    }

    .banner-readonly {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 24px;
      background: #ffebee;
      color: #c62828;
      font-size: 14px;
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
      background: #fff3e0;
      color: #e65100;
      font-size: 14px;
    }

    .banner-trial mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }
  `],
})
export class AppComponent implements OnInit {
  constructor(
    public auth: AuthService,
    private subscriptionService: SubscriptionService,
    private snackBar: MatSnackBar
  ) {}

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
}
