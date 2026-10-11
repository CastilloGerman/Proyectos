import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { PresupuestoEnlaceEstado } from '../../../core/models/presupuesto.model';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { HintBannerComponent } from '../../../shared/hint-banner/hint-banner.component';
import { EstadoBadgeComponent } from '../../../shared/estado-badge/estado-badge.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { normalizarTelefonoInternacional } from '../enviar-presupuesto/enviar-presupuesto.component';

const LOCALE_MAP: Record<string, string> = {
  es: 'es-ES', en: 'en-US', fr: 'fr-FR', ro: 'ro-RO', uk: 'uk-UA',
};

@Component({
  selector: 'app-presupuesto-seguimiento',
  standalone: true,
  imports: [
    CommonModule, RouterLink, TranslateModule, HintBannerComponent, EstadoBadgeComponent,
    MatButtonModule, MatIconModule, MatTooltipModule, MatDividerModule, MatDialogModule,
  ],
  providers: [DatePipe],
  template: `
    <section class="tracking" aria-labelledby="tracking-title">
      <h2 id="tracking-title">{{ 'publicBudget.tracking' | translate }}</h2>
      <app-hint-banner
        storageKey="public_budget_link_hint_v1"
        [title]="'publicBudget.hintTitle' | translate"
        [steps]="[
          { icon: 'share', text: ('publicBudget.hint1' | translate) },
          { icon: 'open_in_new', text: ('publicBudget.hint2' | translate) },
          { icon: 'notifications_active', text: ('publicBudget.hint3' | translate) }
        ]"
      />
      @if (status) {
        <div class="data-grid">
          <div class="data-item">
            <span class="data-label">{{ 'publicBudget.sent' | translate }}</span>
            <span class="data-value">
              @if (enviadoAt) {
                {{ enviadoAt | date:'short':undefined:locale }}
                @if (canalEnvio) {
                  <mat-icon class="channel-icon" fontSet="material-icons">
                    {{ canalEnvio === 'WHATSAPP' ? 'chat' : 'email' }}
                  </mat-icon>
                }
              } @else {
                —
              }
            </span>
          </div>
          <div class="data-item">
            <span class="data-label">{{ 'est.lbl.budSent' | translate }}</span>
            <span class="data-value">
              @if (status.activo) {
                <app-estado-badge estado="enviado" />
              } @else {
                <app-estado-badge estado="caducado" />
              }
            </span>
          </div>
          <div class="data-item">
            <span class="data-label">{{ 'est.lbl.budViewed' | translate }}</span>
            <span class="data-value">
              @if (status.primeraVistaAt) {
                <app-estado-badge estado="visto" />
                {{ status.numVistas }}
              } @else {
                —
              }
            </span>
          </div>
          <div class="data-item">
            <span class="data-label">{{ 'publicBudget.activeLinks' | translate }}</span>
            <span class="data-value">{{ status.enlacesActivos }}</span>
          </div>
          @if (status.activo && status.expiraAt) {
            <div class="data-item">
              <span class="data-label">{{ 'publicBudget.linkExpires' | translate }}</span>
              <span class="data-value">{{ status.expiraAt | date:'shortDate':undefined:locale }}</span>
            </div>
          }
        </div>
        <p class="notice">{{ 'publicBudget.resendDoesNotRevoke' | translate }}</p>
      }
      @if (alertCount > 0 || lastAlertAt || silenced || respuestaCliente) {
        <section class="follow-up-alerts" aria-label="{{ 'budgetFollowSettings.alertsTitle' | translate }}">
          <h3>{{ 'budgetFollowSettings.alertsTitle' | translate }}</h3>
          <p>{{ 'budgetFollowSettings.alertCount' | translate:{ count: alertCount } }}</p>
          @if (lastAlertAt) {
            <p>{{ 'budgetFollowSettings.lastAlert' | translate:{ date: (lastAlertAt | date:'short':undefined:locale) } }}</p>
          }
          @if (silenced) {
            <p>{{ 'budgetFollowSettings.silenced' | translate }}</p>
          }
          @if (respuestaCliente) {
            <div class="client-response-notice" role="status">
              <h4>{{ 'budgetFollowSettings.clientResponseTitle' | translate }}</h4>
              <p>{{ 'budgetFollowSettings.clientResponse' | translate:{ option: (('publicBudget.option' + respuestaCliente) | translate), date: (respuestaClienteAt ? (respuestaClienteAt | date:'short':undefined:locale) : '') } }}</p>
              @if (respuestaClienteMensaje) {
                <p class="response-message">{{ 'budgetFollowSettings.clientMessage' | translate }}</p>
                <blockquote>{{ respuestaClienteMensaje }}</blockquote>
              }
            </div>
          }
        </section>
      }
      <div class="actions">
        <button mat-stroked-button [routerLink]="['/presupuestos', presupuestoId]"
          [matTooltip]="'budgetFollowSettings.openBudget' | translate">
          <mat-icon>open_in_new</mat-icon>{{ 'budgetFollowSettings.openBudget' | translate }}
        </button>
        @if (phoneHref) {
          <a [href]="phoneHref" mat-stroked-button [attr.aria-label]="'budgetFollowSettings.callClient' | translate">
            <mat-icon>call</mat-icon>{{ 'budgetFollowSettings.callClient' | translate }}
          </a>
        }
        <button mat-stroked-button type="button" (click)="resend.emit()"
          [matTooltip]="'budgetFollowSettings.resend' | translate">
          <mat-icon>redo</mat-icon>{{ 'budgetFollowSettings.resend' | translate }}
        </button>
        <button mat-button type="button" [disabled]="silencing" (click)="toggleSilenced()">
          {{ (silenced ? 'budgetFollowSettings.reactivate' : 'budgetFollowSettings.silence') | translate }}
        </button>
      </div>
      <mat-divider />
      <div class="public-links-section">
        <h3>{{ 'publicBudget.createLink' | translate }}</h3>
        <div class="actions">
          <button mat-stroked-button type="button" (click)="create()" [disabled]="loading">
            @if (loading) { <mat-icon class="spin">sync</mat-icon> } @else { <mat-icon>link</mat-icon> }
            {{ 'publicBudget.createLink' | translate }}
          </button>
          @if (status?.activo) {
            <button mat-stroked-button type="button" (click)="regenerate()" [disabled]="loading">
              <mat-icon>autorenew</mat-icon>{{ 'publicBudget.regenerateLink' | translate }}
            </button>
          }
          @if (url) {
            <button mat-button type="button" (click)="copy()">
              <mat-icon>content_copy</mat-icon>{{ 'publicBudget.copyLink' | translate }}
            </button>
          }
          @if (status?.activo) {
            <button mat-stroked-button color="warn" type="button" (click)="revoke()" [disabled]="loading">
              <mat-icon>block</mat-icon>{{ 'publicBudget.revokeLink' | translate }}
            </button>
          }
        </div>
        @if (url) { <p class="url">{{ url }}</p> }
      </div>
      <p class="notice">{{ 'publicBudget.linkNotice' | translate }}</p>
    </section>
  `,
  styles: [`
    .tracking { display: grid; gap: var(--app-space-md); }
    h2, h3 { margin: 0; font-weight: 600; }
    h3 { font-size: 1rem; }
    .data-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: var(--app-space-md); }
    .data-item { display: flex; flex-direction: column; gap: 4px; }
    .data-label { font-size: 0.75rem; color: var(--app-text-secondary); }
    .data-value { font-size: 0.875rem; font-weight: 500; display: flex; align-items: center; gap: 6px; }
    .channel-icon { font-size: 16px; width: 16px; height: 16px; }
    .notice { margin: 0; color: var(--app-text-secondary); font-size: 0.75rem; }
    .follow-up-alerts { display: grid; gap: 8px; padding: var(--app-space-md); border-left: 3px solid var(--app-accent-copper); background: color-mix(in srgb, var(--app-accent-copper) 6%, var(--app-bg-card)); border-radius: var(--app-radius-sm); }
    .follow-up-alerts h3, .follow-up-alerts p { margin: 0; }
    .client-response-notice { display: grid; gap: 6px; padding-top: 8px; }
    .client-response-notice h4 { margin: 0; }
    .client-response-notice blockquote { margin: 0; padding-left: 12px; border-left: 2px solid var(--app-border); overflow-wrap: anywhere; white-space: pre-wrap; }
    .response-message { font-weight: 600; }
    .actions { display: flex; flex-wrap: wrap; gap: var(--app-space-sm); }
    .public-links-section { display: grid; gap: var(--app-space-sm); }
    .url { overflow-wrap: anywhere; font-size: 0.8125rem; color: var(--app-text-secondary); }
    .spin { animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class PresupuestoSeguimientoComponent implements OnInit {
  @Input({ required: true }) presupuestoId!: number;
  @Input() enviadoAt: string | null | undefined;
  @Input() canalEnvio: 'WHATSAPP' | 'EMAIL' | null | undefined;
  @Input() alertCount = 0;
  @Input() lastAlertAt: string | null | undefined;
  @Input() silenced = false;
  @Input() respuestaCliente: 'INTERESA' | 'DUDAS' | null | undefined;
  @Input() respuestaClienteAt: string | null | undefined;
  @Input() respuestaClienteMensaje: string | null | undefined;
  @Input() clientPhone: string | null | undefined;
  @Input() clientCountry: string | null | undefined;
  @Output() resend = new EventEmitter<void>();
  private readonly service = inject(PresupuestoService);
  private readonly translate = inject(TranslateService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly datePipe = inject(DatePipe);
  status: PresupuestoEnlaceEstado | null = null;
  url = '';
  loading = false;
  silencing = false;

  get locale(): string {
    return LOCALE_MAP[this.translate.currentLang] || 'es-ES';
  }

  get phoneHref(): string {
    const phone = this.clientPhone?.trim();
    const normalized = phone ? normalizarTelefonoInternacional(phone, this.clientCountry || 'ES') : '';
    return normalized ? `tel:+${normalized}` : '';
  }

  ngOnInit(): void { this.reload(); }

  create(): void {
    this.loading = true;
    this.service.crearEnlace(this.presupuestoId).subscribe({
      next: response => {
        this.url = response.url;
        this.loading = false;
        this.reload();
      },
      error: error => {
        this.loading = false;
        this.snackBar.open(
          error.status === 409 ? this.translate.instant('publicBudget.linkLimitReached') : this.translate.instant('publicBudget.linkFailed'),
          this.translate.instant('common.close'), { duration: 4000 });
      },
    });
  }

  regenerate(): void {
    const data: ConfirmDialogData = {
      title: this.translate.instant('publicBudget.regenerateLink'),
      message: this.translate.instant('publicBudget.linkRegenerated'),
      confirmLabel: this.translate.instant('publicBudget.regenerateLink'),
      confirmColor: 'warn',
    };
    const ref = this.dialog.open(ConfirmDialogComponent, { data });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.loading = true;
      this.service.regenerarEnlace(this.presupuestoId).subscribe({
        next: response => {
          this.url = response.url;
          this.loading = false;
          this.snackBar.open(this.translate.instant('publicBudget.linkRegenerated'), this.translate.instant('common.close'), { duration: 3000 });
          this.reload();
        },
        error: error => {
          this.loading = false;
          this.snackBar.open(
            error.status === 409 ? this.translate.instant('publicBudget.linkLimitReached') : this.translate.instant('publicBudget.linkFailed'),
            this.translate.instant('common.close'), { duration: 3000 });
        },
      });
    });
  }

  revoke(): void {
    const data: ConfirmDialogData = {
      title: this.translate.instant('publicBudget.revokeLink'),
      message: this.translate.instant('publicBudget.linkRevoked'),
      confirmLabel: this.translate.instant('publicBudget.revokeLink'),
      confirmColor: 'warn',
    };
    const ref = this.dialog.open(ConfirmDialogComponent, { data });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.loading = true;
      this.service.revocarEnlace(this.presupuestoId).subscribe({
        next: () => {
          this.loading = false;
          this.url = '';
          this.snackBar.open(this.translate.instant('publicBudget.linkRevoked'), this.translate.instant('common.close'), { duration: 3000 });
          this.reload();
        },
        error: () => {
          this.loading = false;
          this.snackBar.open(this.translate.instant('publicBudget.linkFailed'), this.translate.instant('common.close'), { duration: 3000 });
        },
      });
    });
  }

  copy(): void {
    void navigator.clipboard.writeText(this.url).then(() => {
      this.snackBar.open(this.translate.instant('publicBudget.linkCopied'), this.translate.instant('common.close'), { duration: 3000 });
    }).catch(() => {
      this.snackBar.open(this.translate.instant('publicBudget.linkFailed'), this.translate.instant('common.close'), { duration: 3000 });
    });
  }

  toggleSilenced(): void {
    if (this.silencing) return;
    this.silencing = true;
    const request = this.silenced
      ? this.service.reactivarSeguimiento(this.presupuestoId)
      : this.service.silenciarSeguimiento(this.presupuestoId);
    request.subscribe({
      next: () => {
        this.silenced = !this.silenced;
        this.snackBar.open(this.translate.instant(this.silenced
          ? 'budgetFollowSettings.silenceSuccess'
          : 'budgetFollowSettings.reactivateSuccess'), this.translate.instant('common.close'), { duration: 3000 });
        this.silencing = false;
      },
      error: () => {
        this.snackBar.open(this.translate.instant('budgetFollowSettings.actionError'), this.translate.instant('common.close'), { duration: 3000 });
        this.silencing = false;
      },
    });
  }

  private reload(): void {
    this.service.estadoEnlace(this.presupuestoId).subscribe({
      next: status => this.status = status,
      error: () => {
        this.snackBar.open(this.translate.instant('publicBudget.linkFailed'), this.translate.instant('common.close'), { duration: 3000 });
      },
    });
  }
}
