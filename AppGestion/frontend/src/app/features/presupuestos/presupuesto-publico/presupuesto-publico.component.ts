import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { PresupuestoPublico } from '../../../core/models/presupuesto.model';
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
