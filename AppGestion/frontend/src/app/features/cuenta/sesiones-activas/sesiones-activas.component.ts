import { Component, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/auth.service';

/**
 * Placeholder de Sesiones activas hasta implementar listado/revocación de sesiones en backend.
 */
@Component({
  selector: 'app-sesiones-activas',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, RouterLink, DatePipe],
  templateUrl: './sesiones-activas.component.html',
  styleUrl: './sesiones-activas.component.scss',
})
export class SesionesActivasComponent {
  private readonly auth = inject(AuthService);

  /** Fecha de caducidad del token actual (si está en la sesión guardada). */
  readonly sessionExpiresAt = computed(() => {
    const exp = this.auth.user()?.expiresAt;
    if (!exp) return null;
    const d = new Date(exp);
    return Number.isNaN(d.getTime()) ? null : d;
  });
}
