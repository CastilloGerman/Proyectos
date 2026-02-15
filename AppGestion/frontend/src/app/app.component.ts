import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AsyncPipe } from '@angular/common';
import { AuthService } from './core/auth/auth.service';

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
  ],
  template: `
    @if (auth.isAuthenticated()) {
      <mat-sidenav-container class="sidenav-container">
        <mat-sidenav #drawer mode="side" opened class="sidenav">
          <mat-toolbar color="primary">AppGestion</mat-toolbar>
          <mat-nav-list>
            <a mat-list-item routerLink="/dashboard" routerLinkActive="active">
              <mat-icon matListItemIcon>dashboard</mat-icon>
              <span matListItemTitle>Dashboard</span>
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
          <mat-toolbar color="primary" class="header-toolbar">
            <span class="spacer"></span>
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

    .content {
      padding: 24px;
    }

    .active {
      background-color: rgba(0, 0, 0, 0.04);
    }
  `],
})
export class AppComponent {
  constructor(public auth: AuthService) {}
}
