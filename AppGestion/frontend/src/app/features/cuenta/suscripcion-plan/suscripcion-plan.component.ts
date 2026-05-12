import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService, UsuarioResponse } from '../../../core/auth/auth.service';
import { SubscriptionService, CheckoutBillingPeriod } from '../../../core/services/subscription.service';
import { SubscriptionDetails } from '../../../core/models/subscription-details.model';
import { environment } from '../../../../environments/environment';
import { DevApiService } from '../../../core/services/dev-api.service';
import { daysFromTodayToDateEnd } from '../../../shared/utils/trial-days.util';
import { finalize } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { messageFromHttpError } from '../../../shared/utils/http-error-message.util';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';
import { TranslateService } from '@ngx-translate/core';

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
        MatButtonToggleModule,
    ],
    templateUrl: './suscripcion-plan.component.html',
    styleUrl: './suscripcion-plan.component.scss'
})
export class SuscripcionPlanComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly subscriptionApi = inject(SubscriptionService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly devApi = inject(DevApiService);

  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly me = signal<UsuarioResponse | null>(null);
  readonly subscriptionDetails = signal<SubscriptionDetails | null>(null);
  readonly openingCheckout = signal(false);
  readonly openingPortal = signal(false);
  readonly checkoutBillingPeriod = signal<CheckoutBillingPeriod>('MONTHLY');

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
      TRIALING: 'Periodo de prueba (Stripe)',
      PAST_DUE: 'Pago pendiente',
      INCOMPLETE: 'Pago incompleto',
      UNPAID: 'Suscripción impagada',
      CANCELED: 'Suscripción cancelada',
    };
    return map[s] ?? s;
  });

  readonly ahorroAnualResumen = computed(() => {
    const d = this.subscriptionDetails();
    if (!d || d.yearlySavingsPercentRounded <= 0) return null;
    return `Facturando al año ahorras aprox. un ${d.yearlySavingsPercentRounded} % frente a 12 meses al precio mensual indicado.`;
  });

  readonly precioMensualFmt = computed(() => this.formatEur(this.subscriptionDetails()?.displayMonthlyPriceEur));
  readonly precioAnualFmt = computed(() => this.formatEur(this.subscriptionDetails()?.displayYearlyPriceEur));

  readonly cancelAlFinalDelPeriodo = computed(
    () => this.subscriptionDetails()?.cancelAtPeriodEnd === true,
  );

  readonly requiereAccionPago = computed(
    () => this.subscriptionDetails()?.requiresPaymentAction === true,
  );

  readonly estadoChipClass = computed(() => {
    const s = this.estadoSuscripcion();
    if (s === 'ACTIVE' || s === 'TRIALING') return 'chip chip--ok';
    if (s === 'TRIAL_ACTIVE') return 'chip chip--trial';
    if (s === 'PAST_DUE' || s === 'INCOMPLETE' || s === 'UNPAID') return 'chip chip--warn';
    if (s === 'TRIAL_EXPIRED' || s === 'CANCELED') return 'chip chip--muted';
    return 'chip';
  });

  readonly puedeEscribir = computed(() => this.me()?.canWrite ?? this.auth.user()?.canWrite ?? false);

  readonly portalDisponible = computed(() => this.me()?.billingPortalAvailable === true);

  /** Checkout: contratar o reactivar sin depender solo del portal. */
  readonly mostrarCheckout = computed(() => {
    const s = this.estadoSuscripcion();
    if (s === 'ACTIVE' || s === 'PAST_DUE' || s === 'TRIALING') return false;
    return (
      s === 'TRIAL_ACTIVE' ||
      s === 'TRIAL_EXPIRED' ||
      s === 'CANCELED' ||
      s === 'INCOMPLETE' ||
      s === 'UNPAID'
    );
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
    if (s === 'ACTIVE') {
      if (this.cancelAlFinalDelPeriodo()) {
        return 'Tu suscripción está activa pero no se renovará al final del periodo actual. Puedes reactivar la renovación en el portal de facturación.';
      }
      return 'Tu suscripción está al corriente. Puedes gestionar facturas y método de pago en el portal de Stripe.';
    }
    if (s === 'TRIALING') {
      return 'Estás en periodo de prueba del plan de pago. Al finalizar se iniciará la facturación según el plan elegido.';
    }
    if (s === 'PAST_DUE') return 'Hay un problema con el último cobro. Actualiza el método de pago en el portal de facturación.';
    if (s === 'INCOMPLETE') {
      if (this.requiereAccionPago()) {
        return 'Stripe necesita que completes la verificación del pago (p. ej. autenticación reforzada). Abre el portal o vuelve a intentar el checkout.';
      }
      return 'El alta del método de pago no se completó. Vuelve a iniciar el pago o usa el portal de facturación.';
    }
    if (s === 'UNPAID') {
      return 'La suscripción tiene pagos adeudados tras varios intentos fallidos. Regulariza el método de pago en Stripe.';
    }
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

  onBillingChange(event: MatButtonToggleChange): void {
    const v = event.value;
    if (v === 'MONTHLY' || v === 'YEARLY') {
      this.checkoutBillingPeriod.set(v);
    }
  }

  refrescar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    forkJoin({
      me: this.auth.refreshUser(),
      details: this.subscriptionApi.getSubscriptionDetails(),
    }).subscribe({
      next: ({ me, details }) => {
        if (me) {
          this.me.set(me);
        } else {
          this.loadError.set(true);
        }
        this.subscriptionDetails.set(details);
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
    const presets = this.snackHttpPresets();
    this.subscriptionApi
      .createCheckoutSession(this.checkoutBillingPeriod())
      .pipe(finalize(() => this.openingCheckout.set(false)))
      .subscribe({
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
          const msg = messageFromHttpError(
            err,
            this.translate.instant('shell.snackbarPaymentErrorFallback'),
            presets,
          );
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 6500 });
        },
      });
  }

  abrirPortal(): void {
    this.openingPortal.set(true);
    const presets = this.snackHttpPresets();
    this.subscriptionApi
      .createPortalSession()
      .pipe(finalize(() => this.openingPortal.set(false)))
      .subscribe({
        next: (res) => {
          const url = res.portalUrl?.trim();
          if (url) {
            window.location.href = url;
          } else {
            this.snackBar.open(
              this.translate.instant('shell.snackbarNoPortalUrl'),
              this.translate.instant('common.close'),
              { duration: 4000 },
            );
          }
        },
        error: (err: unknown) => {
          const msg = messageFromHttpError(
            err,
            this.translate.instant('shell.snackbarPortalErrorFallback'),
            presets,
          );
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 6500 });
        },
      });
  }

  /** Solo desarrollo: alinear con banner del layout. */
  grantPremiumDev(): void {
    const presets = this.snackHttpPresets();
    this.devApi.grantPremium().subscribe({
      next: () => {
        this.auth.refreshUser().subscribe((data) => {
          if (data) this.me.set(data);
          this.snackBar.open(
            this.translate.instant('shell.snackbarDevPremiumOk'),
            this.translate.instant('common.close'),
            { duration: 3000 },
          );
        });
      },
      error: (err) => {
        const msg = messageFromHttpError(
          err,
          this.translate.instant('shell.snackbarDevPremiumUnavailable'),
          presets,
        );
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 4000 });
      },
    });
  }

  private snackHttpPresets() {
    return {
      offline: this.translate.instant('shell.snackbarOffline'),
      server: this.translate.instant('shell.snackbarServerError'),
    };
  }

  private formatEur(value: number | undefined): string {
    if (value === undefined || Number.isNaN(value)) return '—';
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(value);
  }

  private diasRestantesPrueba(): number | null {
    const end = this.me()?.trialEndDate ?? this.auth.user()?.trialEndDate;
    return daysFromTodayToDateEnd(end);
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
