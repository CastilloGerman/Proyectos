import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService, UsuarioResponse } from '../../../core/auth/auth.service';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { environment } from '../../../../environments/environment';

/** Nombre comercial del plan (un solo precio Stripe en esta fase). */
const DEFAULT_PLAN_LABEL = 'Plan profesional';

@Component({
    selector: 'app-suscripcion-plan',
    imports: [
        CommonModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatDividerModule,
    ],
    templateUrl: './suscripcion-plan.component.html',
    styleUrl: './suscripcion-plan.component.scss'
})
export class SuscripcionPlanComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly subscriptionApi = inject(SubscriptionService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly http = inject(HttpClient);

  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly me = signal<UsuarioResponse | null>(null);
  readonly openingCheckout = signal(false);
  readonly openingPortal = signal(false);

  readonly planLabel = environment.subscriptionPlanDisplayName?.trim() || DEFAULT_PLAN_LABEL;

  readonly estadoSuscripcion = computed(() => {
    const s = this.me()?.subscriptionStatus ?? this.auth.user()?.subscriptionStatus;
    return s ?? '—';
  });

  readonly estadoEtiqueta = computed(() => {
    const s = this.estadoSuscripcion();
    const map: Record<string, string> = {
      TRIAL_ACTIVE: 'Prueba gratuita activa',
      TRIAL_EXPIRED: 'Prueba finalizada',
      ACTIVE: 'Suscripción activa',
      PAST_DUE: 'Pago pendiente',
      CANCELED: 'Suscripción cancelada',
    };
    return map[s] ?? s;
  });

  readonly estadoChipClass = computed(() => {
    const s = this.estadoSuscripcion();
    if (s === 'ACTIVE') return 'chip chip--ok';
    if (s === 'TRIAL_ACTIVE') return 'chip chip--trial';
    if (s === 'PAST_DUE') return 'chip chip--warn';
    if (s === 'TRIAL_EXPIRED' || s === 'CANCELED') return 'chip chip--muted';
    return 'chip';
  });

  readonly puedeEscribir = computed(() => this.me()?.canWrite ?? this.auth.user()?.canWrite ?? false);

  readonly portalDisponible = computed(() => this.me()?.billingPortalAvailable === true);

  /** Checkout: contratar o reactivar sin depender solo del portal. */
  readonly mostrarCheckout = computed(() => {
    const s = this.estadoSuscripcion();
    if (s === 'ACTIVE' || s === 'PAST_DUE') return false;
    return s === 'TRIAL_ACTIVE' || s === 'TRIAL_EXPIRED' || s === 'CANCELED';
  });

  readonly textoAyudaEstado = computed(() => {
    const s = this.estadoSuscripcion();
    if (s === 'TRIAL_ACTIVE') {
      const d = this.diasRestantesPrueba();
      if (d === null) return 'Disfruta de todas las funciones durante la prueba.';
      if (d < 0) return 'La fecha de prueba ha pasado; contrata el plan para seguir editando.';
      if (d === 0) return 'Tu prueba termina hoy. Contrata el plan para no perder el acceso completo.';
      return `Te quedan ${d} día${d === 1 ? '' : 's'} de prueba con acceso completo.`;
    }
    if (s === 'ACTIVE') return 'Tu suscripción está al corriente. Puedes gestionar facturas y método de pago en el portal de Stripe.';
    if (s === 'PAST_DUE') return 'Hay un problema con el último cobro. Actualiza el método de pago en el portal de facturación.';
    if (s === 'CANCELED') return 'La suscripción está cancelada. Puedes volver a contratar o revisar opciones en el portal si ya pagaste antes.';
    if (s === 'TRIAL_EXPIRED') return 'La prueba ha terminado. La cuenta queda en solo lectura hasta que contrates el plan.';
    return '';
  });

  readonly fechaFinPruebaFmt = computed(() => this.formatDateOnly(this.me()?.trialEndDate ?? this.auth.user()?.trialEndDate));

  readonly finPeriodoFmt = computed(() => {
    const raw = this.me()?.subscriptionCurrentPeriodEnd;
    return this.formatDateTimeEs(raw);
  });

  protected readonly environment = environment;

  ngOnInit(): void {
    this.refrescar();
  }

  refrescar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.auth.refreshUser().subscribe({
      next: (data) => {
        if (data) {
          this.me.set(data);
        } else {
          this.loadError.set(true);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  abrirCheckout(): void {
    this.openingCheckout.set(true);
    this.subscriptionApi.createCheckoutSession().subscribe({
      next: (res) => {
        if (res.checkoutUrl) {
          window.location.href = res.checkoutUrl;
        } else {
          this.openingCheckout.set(false);
          this.snackBar.open('No se recibió la URL de pago', 'Cerrar', { duration: 4000 });
        }
      },
      error: (err) => {
        this.openingCheckout.set(false);
        this.snackBar.open(err.error?.error || 'No se pudo iniciar el pago', 'Cerrar', { duration: 5000 });
      },
    });
  }

  abrirPortal(): void {
    this.openingPortal.set(true);
    this.subscriptionApi.createPortalSession().subscribe({
      next: (res) => {
        if (res.portalUrl) {
          window.location.href = res.portalUrl;
        } else {
          this.openingPortal.set(false);
          this.snackBar.open('No se recibió el enlace del portal', 'Cerrar', { duration: 4000 });
        }
      },
      error: (err) => {
        this.openingPortal.set(false);
        this.snackBar.open(err.error?.error || 'No se pudo abrir el portal de facturación', 'Cerrar', { duration: 5000 });
      },
    });
  }

  /** Solo desarrollo: alinear con banner del layout. */
  grantPremiumDev(): void {
    this.http.post<{ ok: boolean }>(`${environment.apiUrl}/dev/grant-premium`, {}).subscribe({
      next: () => {
        this.auth.refreshUser().subscribe((data) => {
          if (data) this.me.set(data);
          this.snackBar.open('Premium activado (solo dev)', 'Cerrar', { duration: 3000 });
        });
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'No disponible en este entorno', 'Cerrar', { duration: 4000 });
      },
    });
  }

  private diasRestantesPrueba(): number | null {
    const end = this.me()?.trialEndDate ?? this.auth.user()?.trialEndDate;
    if (!end) return null;
    const endDate = new Date(end);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    endDate.setHours(0, 0, 0, 0);
    return Math.ceil((endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }

  private formatDateOnly(raw: string | undefined): string {
    if (!raw) return '—';
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  private formatDateTimeEs(raw: string | undefined | null): string {
    if (!raw) return '—';
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleString('es-ES', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
