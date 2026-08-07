import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { map } from 'rxjs';
import { AuthService, UsuarioResponse } from '../../../core/auth/auth.service';
import { SubscriptionService, CheckoutBillingPeriod } from '../../../core/services/subscription.service';
import { SubscriptionDetails } from '../../../core/models/subscription-details.model';
import { environment } from '../../../../environments/environment';
import { daysFromTodayToDateEnd } from '../../../shared/utils/trial-days.util';
import { finalize } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { messageFromHttpError } from '../../../shared/utils/http-error-message.util';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';

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
        TranslateModule,
    ],
    templateUrl: './suscripcion-plan.component.html',
    styleUrl: './suscripcion-plan.component.scss'
})
export class SuscripcionPlanComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly subscriptionApi = inject(SubscriptionService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly me = signal<UsuarioResponse | null>(null);
  readonly subscriptionDetails = signal<SubscriptionDetails | null>(null);
  readonly openingCheckout = signal(false);
  readonly openingPortal = signal(false);
  readonly checkoutBillingPeriod = signal<CheckoutBillingPeriod>('MONTHLY');

  private readonly localeTick = toSignal(this.translate.onLangChange.pipe(map(() => Date.now())), {
    initialValue: 0,
  });

  readonly estadoSuscripcion = computed(() => this.me()?.subscriptionStatus ?? this.auth.user()?.subscriptionStatus ?? '');

  readonly planDisplayLabel = computed(() => {
    void this.localeTick();
    const envLabel = environment.subscriptionPlanDisplayName?.trim();
    if (envLabel) return envLabel;
    return this.translate.instant('acctSub.lblDefaultPlan');
  });

  readonly estadoEtiqueta = computed(() => {
    void this.localeTick();
    const s = this.estadoSuscripcion();
    if (!s) return this.translate.instant('acctProf.unknown');
    const keyMap: Record<string, string> = {
      TRIAL_ACTIVE: 'acctSub.stTrialActiveFull',
      TRIAL_EXPIRED: 'acctSub.stTrialEnded',
      ACTIVE: 'acctSub.stActive',
      TRIALING: 'acctSub.stTrialing',
      PAST_DUE: 'acctSub.stPastDue',
      INCOMPLETE: 'acctSub.stIncomplete',
      UNPAID: 'acctSub.stUnpaid',
      CANCELED: 'acctSub.stCanceled',
    };
    const k = keyMap[s];
    return k ? this.translate.instant(k) : s;
  });

  readonly ahorroAnualResumen = computed(() => {
    void this.localeTick();
    const d = this.subscriptionDetails();
    if (!d || d.yearlySavingsPercentRounded <= 0) return null;
    return this.translate.instant('acctSub.pctSave', { pct: d.yearlySavingsPercentRounded });
  });

  readonly precioMensualFmt = computed(() => {
    void this.localeTick();
    return this.formatEur(this.subscriptionDetails()?.displayMonthlyPriceEur);
  });

  readonly precioAnualFmt = computed(() => {
    void this.localeTick();
    return this.formatEur(this.subscriptionDetails()?.displayYearlyPriceEur);
  });

  readonly precioDosIntervalosLbl = computed(() => {
    void this.localeTick();
    return this.translate.instant('acctSub.perMo', {
      v: this.precioMensualFmt(),
      y: this.precioAnualFmt(),
    });
  });

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
    void this.localeTick();
    const s = this.estadoSuscripcion();
    if (s === 'TRIAL_ACTIVE') {
      const d = this.diasRestantesPrueba();
      if (d === null) return this.translate.instant('acctSub.hTrialEnjoy');
      if (d < 0) return this.translate.instant('acctSub.hTrialEnded');
      if (d === 0) return this.translate.instant('acctSub.hTrialToday');
      return this.translate.instant('acctSub.hTrialDays', { days: d });
    }
    if (s === 'ACTIVE') {
      if (this.cancelAlFinalDelPeriodo()) {
        return this.translate.instant('acctSub.hActiveRenewOff');
      }
      return this.translate.instant('acctSub.hActiveOk');
    }
    if (s === 'TRIALING') return this.translate.instant('acctSub.hTrialing');
    if (s === 'PAST_DUE') return this.translate.instant('acctSub.hPastDue');
    if (s === 'INCOMPLETE') {
      if (this.requiereAccionPago()) {
        return this.translate.instant('acctSub.hIncompleteNeedsAction');
      }
      return this.translate.instant('acctSub.hIncompleteGeneric');
    }
    if (s === 'UNPAID') return this.translate.instant('acctSub.hUnpaid');
    if (s === 'CANCELED') return this.translate.instant('acctSub.hCanceled');
    if (s === 'TRIAL_EXPIRED') return this.translate.instant('acctSub.hTrialExpiredRo');
    return '';
  });

  readonly fechaFinPruebaFmt = computed(() => {
    void this.localeTick();
    return this.formatDateOnly(this.me()?.trialEndDate ?? this.auth.user()?.trialEndDate);
  });

  readonly finPeriodoFmt = computed(() => {
    void this.localeTick();
    const raw = this.me()?.subscriptionCurrentPeriodEnd;
    return this.formatDateTimeLocale(raw);
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

  private snackHttpPresets() {
    return {
      offline: this.translate.instant('shell.snackbarOffline'),
      server: this.translate.instant('shell.snackbarServerError'),
    };
  }

  private formatEur(value: number | undefined): string {
    if (value === undefined || Number.isNaN(value)) {
      return this.translate.instant('acctProf.unknown');
    }
    return new Intl.NumberFormat(this.dateLocaleTag(), { style: 'currency', currency: 'EUR' }).format(value);
  }

  private diasRestantesPrueba(): number | null {
    const end = this.me()?.trialEndDate ?? this.auth.user()?.trialEndDate;
    return daysFromTodayToDateEnd(end);
  }

  private formatDateOnly(raw: string | undefined): string {
    if (!raw) return this.translate.instant('acctProf.unknown');
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) return this.translate.instant('acctProf.unknown');
    return d.toLocaleDateString(this.dateLocaleTag(), { day: 'numeric', month: 'long', year: 'numeric' });
  }

  private formatDateTimeLocale(raw: string | undefined | null): string {
    if (!raw) return this.translate.instant('acctProf.unknown');
    const d = new Date(raw);
    if (Number.isNaN(d.getTime())) return this.translate.instant('acctProf.unknown');
    return d.toLocaleString(this.dateLocaleTag(), {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  private dateLocaleTag(): string {
    const lang = (this.translate.currentLang || 'es').split('-')[0].toLowerCase();
    const map: Record<string, string> = {
      es: 'es-ES',
      en: 'en-GB',
      fr: 'fr-FR',
      ro: 'ro-RO',
      uk: 'uk-UA',
    };
    return map[lang] ?? 'es-ES';
  }
}
