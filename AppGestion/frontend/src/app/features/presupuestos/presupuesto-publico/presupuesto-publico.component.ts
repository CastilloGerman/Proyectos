import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PresupuestoPublico, PresupuestoRespuestaClienteOpcion } from '../../../core/models/presupuesto.model';
import { PresupuestoService } from '../../../core/services/presupuesto.service';

@Component({
  selector: 'app-presupuesto-publico',
  standalone: true,
  imports: [CommonModule, TranslateModule],
  template: `
    <main class="public-budget" aria-live="polite">
      @if (loading) {
        <p role="status">{{ 'publicBudget.loading' | translate }}</p>
      } @else if (unavailable) {
        <h1>{{ 'publicBudget.unavailable' | translate }}</h1>
      } @else if (networkError) {
        <h1>{{ 'publicBudget.networkError' | translate }}</h1>
      } @else if (budget) {
        <header class="budget-heading">
          @if (budget.empresaLogoBase64) {
            <img class="company-logo" [src]="'data:' + (budget.empresaLogoMimeType || 'image/png') + ';base64,' + budget.empresaLogoBase64" alt="" />
          }
          <p class="company">{{ budget.empresaNombre }}</p>
          <h1>{{ 'publicBudget.title' | translate }} Nº {{ budget.numero }}</h1>
          <p>{{ budget.fecha | date:'longDate' }}</p>
          <p>{{ 'publicBudget.forCustomer' | translate }} {{ budget.clienteNombre }}</p>
        </header>
        <section [attr.aria-label]="'publicBudget.itemsAria' | translate">
          @for (item of budget.partidas; track $index) {
            <article class="budget-item">
              <div><strong>{{ item.descripcion }}</strong><p>{{ item.cantidad }} {{ item.unidad }} × {{ item.precioUnitario | number:'1.2-2' }} €</p></div>
              <strong>{{ item.subtotal | number:'1.2-2' }} €</strong>
            </article>
          }
        </section>
        <section class="totals">
          <p><span>{{ 'publicBudget.subtotal' | translate }}</span><span>{{ budget.subtotal | number:'1.2-2' }} €</span></p>
          <p><span>{{ 'publicBudget.vat' | translate }}</span><span>{{ budget.iva | number:'1.2-2' }} €</span></p>
          <p class="total"><span>{{ 'publicBudget.total' | translate }}</span><span>{{ budget.total | number:'1.2-2' }} €</span></p>
        </section>
        @if (budget.notas) { <section><h2>{{ 'publicBudget.notes' | translate }}</h2><p>{{ budget.notas }}</p></section> }
        @if (budget.condiciones.length) { <section><h2>{{ 'publicBudget.conditions' | translate }}</h2><p>{{ budget.condiciones.join(', ') }}</p></section> }
        <p class="privacy-note">{{ 'publicBudget.privacy' | translate }}</p>
        @if (budget.permiteResponder) {
          <section class="client-response" aria-labelledby="response-title">
            <h2 id="response-title">{{ 'publicBudget.responseTitle' | translate }}</h2>
            @if (budget.respuestaCliente) {
              <p role="status">{{ 'publicBudget.responseSaved' | translate }}</p>
              <p>{{ 'publicBudget.currentResponse' | translate }} {{ ('publicBudget.option' + budget.respuestaCliente) | translate }}</p>
            } @else {
              <p>{{ 'publicBudget.responsePrompt' | translate }}</p>
            }
            <div class="response-actions">
              <button type="button" [disabled]="responseSubmitting" [attr.aria-pressed]="budget.respuestaCliente === 'INTERESA'" (click)="openResponseDialog('INTERESA', $event)">
                {{ 'publicBudget.interested' | translate }}
              </button>
              <button type="button" [disabled]="responseSubmitting" [attr.aria-pressed]="budget.respuestaCliente === 'DUDAS'" (click)="openResponseDialog('DUDAS', $event)">
                {{ 'publicBudget.haveQuestions' | translate }}
              </button>
            </div>
            <p class="legal-note">{{ 'publicBudget.nonContractual' | translate }}</p>
            @if (responseError) { <p class="response-error" role="alert">{{ responseError | translate }}</p> }
          </section>
        }
        @if (responseDialogOpen) {
          <div class="dialog-backdrop" (click)="onDialogBackdrop($event)">
            <section #responseDialog class="response-dialog" role="dialog" aria-modal="true"
              [attr.aria-labelledby]="'response-dialog-title'" [attr.aria-describedby]="'response-dialog-description'">
              <h2 id="response-dialog-title">{{ 'publicBudget.responseDialogTitle' | translate }}</h2>
              <p id="response-dialog-description">{{ 'publicBudget.responseDialogDescription' | translate:{ option: (('publicBudget.option' + selectedOption) | translate) } }}</p>
              <label for="response-message">{{ 'publicBudget.optionalMessage' | translate }}</label>
              <textarea #responseMessage id="response-message" maxlength="500" rows="4" [value]="messageDraft"
                [disabled]="responseSubmitting" (input)="updateMessage($event)"></textarea>
              <p class="character-count">{{ 'publicBudget.messageCount' | translate:{ count: messageDraft.length } }}</p>
              @if (responseError) { <p class="response-error" role="alert">{{ responseError | translate }}</p> }
              <div class="dialog-actions">
                <button type="button" class="secondary" [disabled]="responseSubmitting" (click)="closeResponseDialog()">{{ 'publicBudget.cancelResponse' | translate }}</button>
                <button type="button" [disabled]="responseSubmitting" (click)="submitResponse()">
                  {{ (responseSubmitting ? 'publicBudget.sendingResponse' : 'publicBudget.sendResponse') | translate }}
                </button>
              </div>
            </section>
          </div>
        }
        <button type="button" class="download" [disabled]="downloading" (click)="download()">{{ 'publicBudget.download' | translate }}</button>
        @if (downloadError) { <p role="alert">{{ 'publicBudget.downloadError' | translate }}</p> }
        @if (viewRegistrationError) { <p role="status">{{ 'publicBudget.viewRegistrationError' | translate }}</p> }
      }
    </main>
  `,
  styles: [`
    :host { display:block; flex:1; }
    .public-budget { box-sizing:border-box; width:min(100%,760px); margin:0 auto; padding:24px 16px 48px; color:#172033; }
    .budget-heading { text-align:center; padding:12px 0 24px; }
    .company-logo { max-width:180px; max-height:88px; object-fit:contain; }
    .company { font-weight:700; }
    .budget-item,.totals p { display:flex; justify-content:space-between; gap:16px; border-bottom:1px solid #dbe1ea; padding:14px 0; }
    .budget-item p { margin:6px 0 0; color:#536071; }
    .totals { margin:16px 0 24px auto; max-width:340px; }
    .total { font-weight:800; font-size:1.15rem; }
    .download { width:100%; min-height:54px; color:white; background:#176b45; border:0; border-radius:10px; font:inherit; font-weight:700; cursor:pointer; }
    .download:disabled { opacity:.6; }
    .privacy-note { margin-top:24px; color:#536071; font-size:.9rem; }
    .client-response { display:grid; gap:12px; margin:24px 0; padding:16px; border:1px solid #dbe1ea; border-radius:12px; background:#f8fafc; }
    .client-response h2,.client-response p { margin:0; }
    .response-actions,.dialog-actions { display:flex; flex-wrap:wrap; gap:10px; }
    .response-actions button,.dialog-actions button { flex:1 1 180px; min-height:48px; padding:10px 14px; border:1px solid #176b45; border-radius:8px; color:#fff; background:#176b45; font:inherit; font-weight:700; cursor:pointer; }
    .response-actions button[aria-pressed="true"] { outline:3px solid #a7d7bf; outline-offset:2px; }
    .response-actions button:disabled,.dialog-actions button:disabled { opacity:.6; cursor:not-allowed; }
    .legal-note,.character-count { color:#536071; font-size:.9rem; }
    .response-error { color:#b42318; }
    .dialog-backdrop { position:fixed; z-index:1000; inset:0; display:grid; place-items:center; padding:16px; background:rgba(15,23,42,.6); }
    .response-dialog { box-sizing:border-box; width:min(100%,480px); display:grid; gap:12px; padding:22px; border-radius:14px; background:#fff; box-shadow:0 18px 50px rgba(0,0,0,.25); }
    .response-dialog h2,.response-dialog p { margin:0; }
    .response-dialog textarea { box-sizing:border-box; width:100%; min-height:110px; padding:10px; border:1px solid #87909d; border-radius:8px; font:inherit; resize:vertical; }
    .response-dialog label { font-weight:700; }
    .dialog-actions button.secondary { color:#172033; border-color:#9aa4b2; background:#fff; }
    h1 { font-size:1.5rem; }
  `],
})
export class PresupuestoPublicoComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(PresupuestoService);
  private readonly meta = inject(Meta);
  budget: PresupuestoPublico | null = null;
  loading = true;
  unavailable = false;
  networkError = false;
  downloading = false;
  downloadError = false;
  viewRegistrationError = false;
  responseDialogOpen = false;
  responseSubmitting = false;
  responseError = '';
  messageDraft = '';
  selectedOption: PresupuestoRespuestaClienteOpcion = 'INTERESA';
  @ViewChild('responseMessage') private responseMessage?: ElementRef<HTMLTextAreaElement>;
  private responseTrigger: HTMLElement | null = null;
  private token = '';
  private viewTimer: ReturnType<typeof setTimeout> | null = null;
  private viewRecorded = false;
  private previousRobots: string | null = null;
  private previousReferrer: string | null = null;

  private readonly onVisibilityChange = (): void => {
    if (document.visibilityState === 'visible') this.scheduleViewRegistration();
    else this.clearViewTimer();
  };
  private readonly onFirstInteraction = (): void => this.recordView();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.responseDialogOpen && !this.responseSubmitting) this.closeResponseDialog();
  }

  ngOnInit(): void {
    this.previousRobots = this.meta.getTag('name="robots"')?.content ?? null;
    this.previousReferrer = this.meta.getTag('name="referrer"')?.content ?? null;
    this.meta.updateTag({ name: 'robots', content: 'noindex,nofollow' });
    this.meta.updateTag({ name: 'referrer', content: 'no-referrer' });
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    this.service.getPublico(this.token).subscribe({
      next: budget => {
        this.budget = budget;
        this.loading = false;
        document.addEventListener('visibilitychange', this.onVisibilityChange);
        document.addEventListener('pointerdown', this.onFirstInteraction, { once: true });
        document.addEventListener('keydown', this.onFirstInteraction, { once: true });
        document.addEventListener('touchstart', this.onFirstInteraction, { once: true });
        this.scheduleViewRegistration();
      },
      error: error => {
        this.loading = false;
        if (error.status === 404) this.unavailable = true;
        else this.networkError = true;
      },
    });
  }

  ngOnDestroy(): void {
    this.clearViewTimer();
    document.removeEventListener('visibilitychange', this.onVisibilityChange);
    document.removeEventListener('pointerdown', this.onFirstInteraction);
    document.removeEventListener('keydown', this.onFirstInteraction);
    document.removeEventListener('touchstart', this.onFirstInteraction);
    this.restoreMeta('robots', this.previousRobots);
    this.restoreMeta('referrer', this.previousReferrer);
  }

  private scheduleViewRegistration(): void {
    this.clearViewTimer();
    if (this.viewRecorded || document.visibilityState !== 'visible') return;
    this.viewTimer = setTimeout(() => this.recordView(), 3000);
  }

  private recordView(): void {
    if (this.viewRecorded || !this.budget) return;
    this.viewRecorded = true;
    this.clearViewTimer();
    this.service.marcarPublicoVisto(this.token).subscribe({
      error: () => { this.viewRegistrationError = true; },
    });
  }

  private clearViewTimer(): void {
    if (this.viewTimer !== null) {
      clearTimeout(this.viewTimer);
      this.viewTimer = null;
    }
  }

  private restoreMeta(name: string, content: string | null): void {
    if (content === null) this.meta.removeTag(`name="${name}"`);
    else this.meta.updateTag({ name, content });
  }

  openResponseDialog(option: PresupuestoRespuestaClienteOpcion, event: MouseEvent): void {
    if (this.responseSubmitting || !this.budget?.permiteResponder) return;
    this.selectedOption = option;
    this.messageDraft = '';
    this.responseError = '';
    this.responseTrigger = event.currentTarget as HTMLElement;
    this.responseDialogOpen = true;
    setTimeout(() => this.responseMessage?.nativeElement.focus());
  }

  updateMessage(event: Event): void {
    this.messageDraft = (event.target as HTMLTextAreaElement).value.slice(0, 500);
  }

  onDialogBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget && !this.responseSubmitting) this.closeResponseDialog();
  }

  closeResponseDialog(): void {
    if (this.responseSubmitting) return;
    this.responseDialogOpen = false;
    this.responseTrigger?.focus();
    this.responseTrigger = null;
  }

  submitResponse(): void {
    if (this.responseSubmitting || !this.responseDialogOpen) return;
    this.responseSubmitting = true;
    this.responseError = '';
    const message = this.messageDraft.trim();
    this.service.responderPublico(this.token, {
      opcion: this.selectedOption,
      ...(message ? { mensaje: message } : {}),
    }).subscribe({
      next: () => {
        if (this.budget) this.budget = { ...this.budget, respuestaCliente: this.selectedOption };
        this.responseSubmitting = false;
        this.responseDialogOpen = false;
        this.responseTrigger?.focus();
        this.responseTrigger = null;
      },
      error: error => {
        this.responseSubmitting = false;
        this.responseError = error.status === 404
          ? 'publicBudget.responseUnavailable'
          : error.status === 429
            ? 'publicBudget.responseRateLimited'
            : 'publicBudget.responseFailed';
      },
    });
  }

  download(): void {
    this.downloading = true;
    this.downloadError = false;
    this.service.descargarPdfPublico(this.token).subscribe({
      next: blob => {
        this.downloading = false;
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `presupuesto-${this.budget?.numero ?? ''}.pdf`;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.downloading = false; this.downloadError = true; },
    });
  }
}
