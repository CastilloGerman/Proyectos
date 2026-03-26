import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/auth/auth.service';
import { SesionDispositivoDto } from '../../../core/auth/models/auth.model';

@Component({
    selector: 'app-sesiones-activas',
    imports: [
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatChipsModule,
        MatDividerModule,
        RouterLink,
        DatePipe,
    ],
    templateUrl: './sesiones-activas.component.html',
    styleUrl: './sesiones-activas.component.scss'
})
export class SesionesActivasComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly sessions = signal<SesionDispositivoDto[]>([]);
  readonly revokingId = signal<string | null>(null);
  readonly revokingOthers = signal(false);

  readonly sessionExpiresAt = computed(() => {
    const exp = this.auth.user()?.expiresAt;
    if (!exp) return null;
    const d = new Date(exp);
    return Number.isNaN(d.getTime()) ? null : d;
  });

  readonly hasLegacySession = computed(
    () => !!this.auth.getToken() && !this.auth.user()?.sessionId
  );

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.auth.listSessions().subscribe({
      next: (list) => {
        this.sessions.set(list ?? []);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(err.error?.message || err.error?.error || 'No se pudieron cargar las sesiones.');
        this.loading.set(false);
      },
    });
  }

  tipoDispositivoLabel(t: string | null | undefined): string {
    switch (t) {
      case 'MOBILE':
        return 'Móvil';
      case 'TABLET':
        return 'Tablet';
      case 'DESKTOP':
        return 'PC / escritorio';
      default:
        return t || '—';
    }
  }

  cerrarOtraSesion(s: SesionDispositivoDto): void {
    if (s.currentSession) {
      return;
    }
    this.revokingId.set(s.id);
    this.auth.revokeSession(s.id).subscribe({
      next: () => {
        this.revokingId.set(null);
        this.snackBar.open('Sesión cerrada en ese dispositivo.', 'Cerrar', { duration: 3500 });
        this.reload();
      },
      error: (err) => {
        this.revokingId.set(null);
        this.snackBar.open(err.error?.message || err.error?.error || 'No se pudo cerrar la sesión.', 'Cerrar', {
          duration: 5000,
        });
      },
    });
  }

  cerrarResto(): void {
    if (!confirm('¿Cerrar sesión en todos los demás dispositivos? Esta sesión (este navegador) seguirá activa.')) {
      return;
    }
    this.revokingOthers.set(true);
    this.auth.revokeOtherSessions().subscribe({
      next: (res) => {
        this.revokingOthers.set(false);
        const n = res?.revokedCount ?? 0;
        this.snackBar.open(n > 0 ? `Se cerraron ${n} sesión(es).` : 'No había otras sesiones activas.', 'Cerrar', {
          duration: 4000,
        });
        this.reload();
      },
      error: (err) => {
        this.revokingOthers.set(false);
        this.snackBar.open(err.error?.message || err.error?.error || 'No se pudieron cerrar las otras sesiones.', 'Cerrar', {
          duration: 5000,
        });
      },
    });
  }
}
