import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PresupuestoEnlaceEstado } from '../../../core/models/presupuesto.model';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { HintBannerComponent } from '../../../shared/hint-banner/hint-banner.component';
import { normalizarTelefonoInternacional } from '../enviar-presupuesto/enviar-presupuesto.component';

@Component({
  selector: 'app-presupuesto-seguimiento',
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule, HintBannerComponent],
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
        [note]="'publicBudget.linkNotice' | translate"
      />
      @if (enviadoAt) {
        <p>{{ 'publicBudget.sent' | translate:{ date: (enviadoAt | date:'short'), channel: ((canalEnvio === 'EMAIL' ? 'publicBudget.email' : 'publicBudget.whatsapp') | translate) } }}</p>
      }
      @if (status) {
        <p>{{ 'publicBudget.activeLinks' | translate:{ count: status.enlacesActivos } }}</p>
        <p>{{ 'publicBudget.resendDoesNotRevoke' | translate }}</p>
        @if (status.primeraVistaAt) {
          <p>{{ 'publicBudget.viewed' | translate:{ date: (status.primeraVistaAt | date:'short'), count: status.numVistas } }}</p>
        } @else {
          <p>{{ 'publicBudget.notViewed' | translate }}</p>
        }
        @if (alertCount > 0 || lastAlertAt || silenced) {
          <section class="follow-up-alerts" aria-label="{{ 'budgetFollowSettings.alertsTitle' | translate }}">
            <h3>{{ 'budgetFollowSettings.alertsTitle' | translate }}</h3>
            <p>{{ 'budgetFollowSettings.alertCount' | translate:{ count: alertCount } }}</p>
            @if (lastAlertAt) {
              <p>{{ 'budgetFollowSettings.lastAlert' | translate:{ date: (lastAlertAt | date:'short') } }}</p>
            }
            @if (silenced) {
              <p>{{ 'budgetFollowSettings.silenced' | translate }}</p>
            }
          </section>
        }
        <div class="actions">
          <a [routerLink]="['/presupuestos', presupuestoId]">{{ 'budgetFollowSettings.openBudget' | translate }}</a>
          @if (phoneHref) {
            <a [href]="phoneHref" [attr.aria-label]="'budgetFollowSettings.callClient' | translate">
              {{ 'budgetFollowSettings.callClient' | translate }}
            </a>
          }
          <button type="button" (click)="resend.emit()">{{ 'budgetFollowSettings.resend' | translate }}</button>
          <button type="button" [disabled]="silencing" (click)="toggleSilenced()">
            {{ (silenced ? 'budgetFollowSettings.reactivate' : 'budgetFollowSettings.silence') | translate }}
          </button>
        </div>
        @if (status.activo && status.expiraAt) {
          <p>{{ 'publicBudget.linkExpires' | translate:{ date: (status.expiraAt | date:'shortDate') } }}</p>
        }
      }
      <p class="notice">{{ 'publicBudget.linkNotice' | translate }}</p>
      <div class="actions">
        <button type="button" (click)="create()" [disabled]="loading">{{ 'publicBudget.createLink' | translate }}</button>
        @if (status?.activo) {
          <button type="button" (click)="regenerate()" [disabled]="loading">{{ 'publicBudget.regenerateLink' | translate }}</button>
        }
        @if (url) { <button type="button" (click)="copy()">{{ 'publicBudget.copyLink' | translate }}</button> }
        @if (status?.activo) { <button type="button" (click)="revoke()" [disabled]="loading">{{ 'publicBudget.revokeLink' | translate }}</button> }
      </div>
      @if (url) { <p class="url">{{ url }}</p> }
      @if (feedback) { <p role="status">{{ feedback }}</p> }
    </section>
  `,
  styles: [`
    .tracking { display:grid; gap:10px; margin:20px 0; padding:16px; border:1px solid #d7dee8; border-radius:12px; }
    h2,p { margin:0; }
    .notice { color:#596579; font-size:.92rem; }
    .follow-up-alerts { display:grid; gap:8px; padding:12px; border-left:3px solid #d97706; background:rgba(217,119,6,.06); }
    .follow-up-alerts h3, .follow-up-alerts p { margin:0; }
    .actions { display:flex; flex-wrap:wrap; gap:8px; }
    .actions button, .actions a { display:inline-flex; align-items:center; min-height:44px; padding:8px 12px; border:1px solid #718096; border-radius:7px; background:transparent; color:inherit; font:inherit; cursor:pointer; }
    .actions a { text-decoration:none; }
    .url { overflow-wrap:anywhere; }
  `],
})
export class PresupuestoSeguimientoComponent implements OnInit {
  @Input({ required: true }) presupuestoId!: number;
  @Input() enviadoAt: string | null | undefined;
  @Input() canalEnvio: 'WHATSAPP' | 'EMAIL' | null | undefined;
  @Input() alertCount = 0;
  @Input() lastAlertAt: string | null | undefined;
  @Input() silenced = false;
  @Input() clientPhone: string | null | undefined;
  @Input() clientCountry: string | null | undefined;
  @Output() resend = new EventEmitter<void>();
  private readonly service = inject(PresupuestoService);
  private readonly translate = inject(TranslateService);
  status: PresupuestoEnlaceEstado | null = null;
  url = '';
  feedback = '';
  loading = false;
  silencing = false;

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
        this.feedback = error.status === 409
          ? this.translate.instant('publicBudget.linkLimitReached')
          : this.translate.instant('publicBudget.linkFailed');
      },
    });
  }

  regenerate(): void {
    this.loading = true;
    this.service.regenerarEnlace(this.presupuestoId).subscribe({
      next: response => {
        this.url = response.url;
        this.loading = false;
        this.feedback = this.translate.instant('publicBudget.linkRegenerated');
        this.reload();
      },
      error: error => {
        this.loading = false;
        this.feedback = error.status === 409
          ? this.translate.instant('publicBudget.linkLimitReached')
          : this.translate.instant('publicBudget.linkFailed');
      },
    });
  }

  revoke(): void {
    this.loading = true;
    this.service.revocarEnlace(this.presupuestoId).subscribe({
      next: () => {
        this.loading = false;
        this.url = '';
        this.feedback = this.translate.instant('publicBudget.linkRevoked');
        this.reload();
      },
      error: () => { this.loading = false; this.feedback = this.translate.instant('publicBudget.linkFailed'); },
    });
  }

  copy(): void {
    void navigator.clipboard.writeText(this.url).then(() => {
      this.feedback = this.translate.instant('publicBudget.linkCopied');
    }).catch(() => { this.feedback = this.translate.instant('publicBudget.linkFailed'); });
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
        this.feedback = this.translate.instant(this.silenced
          ? 'budgetFollowSettings.silenceSuccess'
          : 'budgetFollowSettings.reactivateSuccess');
        this.silencing = false;
      },
      error: () => {
        this.feedback = this.translate.instant('budgetFollowSettings.actionError');
        this.silencing = false;
      },
    });
  }

  private reload(): void {
    this.service.estadoEnlace(this.presupuestoId).subscribe({
      next: status => this.status = status,
      error: () => { this.feedback = this.translate.instant('publicBudget.linkFailed'); },
    });
  }
}
