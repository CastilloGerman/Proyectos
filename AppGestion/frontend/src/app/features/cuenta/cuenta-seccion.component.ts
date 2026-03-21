import { Component, OnDestroy, computed, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Subject, map, takeUntil } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';

/** Títulos amigables por slug de URL (alineados con user-menu.config). */
export const CUENTA_SECCION_TITLES: Record<string, string> = {
  perfil: 'Perfil',
  'configuracion-cuenta': 'Configuración de cuenta',
  preferencias: 'Preferencias',
  'datos-empresa': 'Datos de la empresa',
  suscripcion: 'Suscripción y plan actual',
  'historial-suscripcion': 'Historial de facturas de la suscripción',
  'metodos-pago': 'Métodos de pago',
  'datos-fiscales': 'Datos fiscales',
  'cambiar-contrasena': 'Cambiar contraseña',
  '2fa': 'Autenticación en dos factores',
  'sesiones-activas': 'Sesiones activas',
  'historial-accesos': 'Historial de accesos',
  'logo-empresa': 'Logo de la empresa',
  plantillas: 'Plantillas',
  impuestos: 'Configuración de impuestos',
  'centro-ayuda': 'Centro de ayuda',
};

@Component({
  selector: 'app-cuenta-seccion',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, RouterLink, DatePipe],
  template: `
    @if (currentSlug === 'plantillas') {
      <div class="rich-wrap">
        <a mat-button routerLink="/dashboard" class="rich-back">
          <mat-icon>arrow_back</mat-icon>
          Volver
        </a>
        <mat-card class="rich-card">
          <div class="rich-card__icon" aria-hidden="true">
            <mat-icon>description</mat-icon>
          </div>
          <h1 class="rich-title">Plantillas de presupuestos y facturas</h1>
          <p class="rich-lead">
            Estamos preparando esta sección. Pronto podrás configurarla desde aquí sin salir de la app.
          </p>
          <p class="rich-hint">
            <mat-icon class="rich-hint__icon">info</mat-icon>
            Mientras tanto, el aspecto de tus PDF depende de los datos de empresa, el logo, la firma y los textos al
            pie que ya puedes editar en otras pantallas. Así tus documentos siguen saliendo con tu marca.
          </p>
          <div class="rich-actions">
            <button mat-stroked-button type="button" routerLink="/dashboard">Ir al panel</button>
            <button mat-button type="button" color="primary" routerLink="/cuenta/datos-empresa">
              Datos de la empresa
            </button>
            <button mat-button type="button" routerLink="/cuenta/datos-fiscales">Datos fiscales</button>
          </div>
        </mat-card>
      </div>
    } @else if (currentSlug === 'sesiones-activas') {
      <div class="rich-wrap">
        <a mat-button routerLink="/dashboard" class="rich-back">
          <mat-icon>arrow_back</mat-icon>
          Volver
        </a>
        <mat-card class="rich-card">
          <div class="rich-card__icon" aria-hidden="true">
            <mat-icon>devices</mat-icon>
          </div>
          <h1 class="rich-title">Sesiones activas</h1>
          <p class="rich-lead">
            Estamos preparando esta sección. Pronto podrás configurarla desde aquí sin salir de la app.
          </p>
          @if (sessionExpiresAt(); as exp) {
            <p class="rich-hint">
              <mat-icon class="rich-hint__icon">schedule</mat-icon>
              Tu sesión en este dispositivo caduca aproximadamente el
              <strong>{{ exp | date: 'short' }}</strong>.
            </p>
          } @else {
            <p class="rich-hint rich-hint--muted">
              Mientras tanto, puedes <strong>cerrar sesión</strong> desde el menú de usuario si usas un equipo
              compartido.
            </p>
          }
          <div class="rich-actions">
            <button mat-stroked-button type="button" routerLink="/dashboard">Ir al panel</button>
            <button mat-button type="button" color="primary" routerLink="/cuenta/cambiar-contrasena">
              Cambiar contraseña
            </button>
          </div>
        </mat-card>
      </div>
    } @else if (currentSlug === 'historial-accesos') {
      <div class="rich-wrap">
        <a mat-button routerLink="/dashboard" class="rich-back">
          <mat-icon>arrow_back</mat-icon>
          Volver
        </a>
        <mat-card class="rich-card">
          <div class="rich-card__icon" aria-hidden="true">
            <mat-icon>history</mat-icon>
          </div>
          <h1 class="rich-title">Historial de accesos</h1>
          <p class="rich-lead">
            Estamos preparando esta sección. Pronto podrás configurarla desde aquí sin salir de la app.
          </p>
          <p class="rich-hint">
            <mat-icon class="rich-hint__icon">info</mat-icon>
            Aquí mostraremos un registro de tus inicios de sesión recientes para que puedas detectar actividad
            inusual. Mientras tanto, protege tu cuenta con una contraseña segura y, si quieres, activa la verificación
            en dos pasos.
          </p>
          <div class="rich-actions">
            <button mat-stroked-button type="button" routerLink="/dashboard">Ir al panel</button>
            <button mat-button type="button" color="primary" routerLink="/cuenta/2fa">Autenticación en dos factores</button>
            <button mat-button type="button" routerLink="/cuenta/sesiones-activas">Sesiones activas</button>
          </div>
        </mat-card>
      </div>
    } @else {
      <div class="cuenta-wrap">
        <mat-card class="cuenta-card">
          <div class="cuenta-card__head">
            <h1 class="cuenta-title">{{ title }}</h1>
            <p class="cuenta-lead">
              Estamos preparando esta sección. Pronto podrás configurarla desde aquí sin salir de la app.
            </p>
            <button mat-stroked-button type="button" routerLink="/dashboard">
              <mat-icon>arrow_back</mat-icon>
              Volver al panel
            </button>
          </div>
        </mat-card>
      </div>
    }
  `,
  styles: `
    .cuenta-wrap {
      max-width: 560px;
      margin: 0 auto;
    }
    .cuenta-card {
      padding: 24px;
      border-radius: var(--app-radius-md, 12px);
    }
    .cuenta-title {
      font-size: 1.35rem;
      font-weight: 600;
      margin: 0 0 8px;
      color: var(--app-text-primary, #0f172a);
    }
    .cuenta-lead {
      margin: 0 0 20px;
      color: var(--app-text-secondary, #64748b);
      line-height: 1.5;
      font-size: 15px;
    }
    button mat-icon {
      margin-right: 6px;
      vertical-align: middle;
    }

    .rich-wrap {
      max-width: 520px;
      margin: 0 auto;
      padding-bottom: 40px;
    }
    .rich-back {
      margin-bottom: 16px;
    }
    .rich-card {
      padding: 28px 24px 24px;
      border-radius: var(--app-radius-md, 12px);
      text-align: center;
    }
    .rich-card__icon {
      display: flex;
      justify-content: center;
      margin-bottom: 12px;
    }
    .rich-card__icon mat-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: var(--mat-sys-primary, #1e40af);
      opacity: 0.9;
    }
    .rich-title {
      margin: 0 0 12px;
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
    }
    .rich-lead {
      margin: 0 0 20px;
      color: var(--app-text-secondary, #64748b);
      line-height: 1.55;
      font-size: 16px;
      text-align: left;
    }
    .rich-hint {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      margin: 0 0 24px;
      padding: 12px 14px;
      border-radius: 10px;
      background: rgba(30, 64, 175, 0.06);
      border: 1px solid rgba(30, 64, 175, 0.12);
      color: var(--app-text-primary, #334155);
      font-size: 14px;
      line-height: 1.45;
      text-align: left;
    }
    .rich-hint--muted {
      background: #f8fafc;
      border-color: rgba(0, 0, 0, 0.06);
    }
    .rich-hint__icon {
      flex-shrink: 0;
      margin-top: 2px;
      font-size: 20px;
      width: 20px;
      height: 20px;
      color: var(--mat-sys-primary, #1e40af);
    }
    .rich-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      justify-content: center;
    }
  `,
})
export class CuentaSeccionComponent implements OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly destroy$ = new Subject<void>();

  title = 'Cuenta';
  currentSlug = '';

  readonly sessionExpiresAt = computed(() => {
    const exp = this.auth.user()?.expiresAt;
    if (!exp) return null;
    const d = new Date(exp);
    return Number.isNaN(d.getTime()) ? null : d;
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map((pm) => pm.get('slug') ?? ''),
        takeUntil(this.destroy$)
      )
      .subscribe((slug) => {
        this.currentSlug = slug;
        this.title = CUENTA_SECCION_TITLES[slug] || this.humanize(slug);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private humanize(slug: string): string {
    if (!slug) return 'Cuenta';
    return slug
      .split('-')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }
}
