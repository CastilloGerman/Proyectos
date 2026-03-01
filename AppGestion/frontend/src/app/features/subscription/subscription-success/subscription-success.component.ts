import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-subscription-success',
  standalone: true,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="container">
      <mat-card class="card">
        <mat-card-header>
          <mat-icon mat-card-avatar color="primary">check_circle</mat-icon>
          <mat-card-title>Suscripción activada</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>Tu suscripción se ha activado correctamente. Ya puedes usar todas las funciones.</p>
        </mat-card-content>
        <mat-card-actions>
          <button mat-raised-button color="primary" routerLink="/dashboard">
            Ir al dashboard
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .container {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }

    .card {
      max-width: 400px;
      width: 100%;
    }

    mat-icon[mat-card-avatar] {
      font-size: 48px;
      width: 48px;
      height: 48px;
    }
  `],
})
export class SubscriptionSuccessComponent {
  constructor(private router: Router, private auth: AuthService) {
    if (this.auth.isAuthenticated()) {
      this.auth.refreshUser().subscribe();
    }
  }
}
