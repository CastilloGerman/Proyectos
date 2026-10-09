import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PresupuestoEnlaceEstado } from '../../../core/models/presupuesto.model';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { HintBannerComponent } from '../../../shared/hint-banner/hint-banner.component';

@Component({
  selector: 'app-presupuesto-seguimiento',
  standalone: true,
  imports: [CommonModule, TranslateModule, HintBannerComponent],
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
    .actions { display:flex; flex-wrap:wrap; gap:8px; }
    button { min-height:44px; padding:8px 12px; border:1px solid #718096; border-radius:7px; background:transparent; font:inherit; cursor:pointer; }
    .url { overflow-wrap:anywhere; }
  `],
})
export class PresupuestoSeguimientoComponent implements OnInit {
  @Input({ required: true }) presupuestoId!: number;
  @Input() enviadoAt: string | null | undefined;
  @Input() canalEnvio: 'WHATSAPP' | 'EMAIL' | null | undefined;
  private readonly service = inject(PresupuestoService);
  private readonly translate = inject(TranslateService);
  status: PresupuestoEnlaceEstado | null = null;
  url = '';
  feedback = '';
  loading = false;

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

  private reload(): void {
    this.service.estadoEnlace(this.presupuestoId).subscribe({
      next: status => this.status = status,
      error: () => { this.feedback = this.translate.instant('publicBudget.linkFailed'); },
    });
  }
}
