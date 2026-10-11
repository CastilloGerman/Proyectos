import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { Presupuesto } from '../../../core/models/presupuesto.model';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { AuthService } from '../../../core/auth/auth.service';
import { firstValueFrom } from 'rxjs';

const COUNTRY_PREFIX: Record<string, string> = {
  ES: '34', ESPANA: '34', SPAIN: '34', FR: '33', FRANCE: '33', PT: '351', PORTUGAL: '351',
  IT: '39', ITALY: '39', DE: '49', GERMANY: '49', ALEMANIA: '49', GB: '44', 'UNITED KINGDOM': '44', 'REINO UNIDO': '44',
};

export function normalizarTelefonoInternacional(phone: string, country = 'ES'): string {
  const value = (phone ?? '').trim();
  let digits = value.replace(/\D/g, '');
  if (digits.startsWith('00')) digits = digits.slice(2);
  if (value.startsWith('+') || value.startsWith('00')) return digits;
  const countryKey = country.normalize('NFD').replace(/\p{Diacritic}/gu, '').toUpperCase();
  const prefix = COUNTRY_PREFIX[countryKey] ?? COUNTRY_PREFIX['ES'];
  if (digits.startsWith(prefix) && digits.length > (prefix === '34' ? 9 : prefix.length)) return digits;
  if (prefix === '34' && digits.length === 9) return `${prefix}${digits}`;
  if (prefix === '33' && digits.length === 10 && digits.startsWith('0')) return `${prefix}${digits.slice(1)}`;
  if (prefix === '44' && digits.length === 11 && digits.startsWith('0')) return `${prefix}${digits.slice(1)}`;
  if (prefix === '351' && digits.length === 9) return `${prefix}${digits}`;
  return digits.length >= 8 && digits.length <= 15 ? digits : '';
}

export function construirMensajePresupuesto(p: Presupuesto, contratista: string): string {
  const items = p.items?.map(i => i.descripcion).filter(Boolean).slice(0, 3).join(', ');
  const resumen = items || 'los trabajos indicados';
  const validez = p.condicionesActivas?.includes('validez_30_dias')
    ? 'Válido durante 30 días.'
    : 'Consulta la validez indicada en el PDF.';
  return `Hola ${p.clienteNombre || ''}, te envío el presupuesto para ${resumen}, por un total de ${Number(p.total ?? 0).toFixed(2)} €. ${validez} Un saludo, ${contratista || 'tu contratista'}.`;
}

@Component({
  selector: 'app-enviar-presupuesto',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, TranslateModule,
    MatButtonModule, MatIconModule, MatTooltipModule,
    MatFormFieldModule, MatInputModule, MatCheckboxModule,
  ],
  template: `
    @if (expanded) {
    <section class="send-section" aria-labelledby="share-title">
      <h2 id="share-title">{{ 'budgetShare.title' | translate }}</h2>
      @if (presupuesto.enviadoAt) {
        <p class="sent-state">
          <mat-icon class="channel-icon">{{ presupuesto.canalEnvio === 'WHATSAPP' ? 'chat' : 'email' }}</mat-icon>
          {{ 'budgetShare.sent' | translate }} · {{ presupuesto.enviadoAt | date:'short' }}
          ({{ presupuesto.canalEnvio === 'WHATSAPP' ? ('publicBudget.whatsapp' | translate) : ('publicBudget.email' | translate) }})
        </p>
      }
      @if (telefonoNoGuardado) {
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ 'budgetShare.phone' | translate }}</mat-label>
          <input matInput type="tel" [(ngModel)]="telefono" autocomplete="tel" />
        </mat-form-field>
        <mat-checkbox [(ngModel)]="guardarTelefono">{{ 'budgetShare.savePhone' | translate }}</mat-checkbox>
      }
      <div class="send-buttons">
        <button mat-raised-button color="primary" type="button" [disabled]="loading" (click)="compartirWhatsApp()"
          [matTooltip]="'budgetShare.whatsapp' | translate">
          @if (loading) { <mat-icon class="spin">sync</mat-icon> } @else { <mat-icon class="wa-icon">chat</mat-icon> }
          {{ 'budgetShare.whatsapp' | translate }}
        </button>
        <button mat-stroked-button type="button" [disabled]="loading" (click)="mostrarEmail = !mostrarEmail">
          <mat-icon>email</mat-icon>
          {{ 'budgetShare.email' | translate }}
        </button>
      </div>
      @if (waUrl) {
        <a [href]="waUrl" target="_blank" rel="noopener" (click)="prepararFallback()">
          {{ 'budgetShare.openWhatsApp' | translate }}
        </a>
        @if (fallbackPendiente) {
          <button mat-button type="button" (click)="confirmarEnviado()">
            <mat-icon>check</mat-icon>{{ 'budgetShare.alreadySent' | translate }}
          </button>
        }
      }
      <div class="secondary-buttons">
        <button mat-button type="button" (click)="copiarMensaje()">
          <mat-icon>content_copy</mat-icon>{{ 'budgetShare.copy' | translate }}
        </button>
        <button mat-button type="button" [disabled]="loading" (click)="descargarPdf()">
          @if (loading) { <mat-icon class="spin">sync</mat-icon> } @else { <mat-icon>download</mat-icon> }
          {{ 'budgetShare.download' | translate }}
        </button>
      </div>
      @if (mostrarEmail) {
        <form [formGroup]="emailForm" (ngSubmit)="enviarEmail()" class="email-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'budgetShare.recipient' | translate }}</mat-label>
            <input matInput formControlName="recipient" type="email" maxlength="254" />
            @if (emailForm.get('recipient')?.hasError('required') && emailForm.get('recipient')?.touched) {
              <mat-error>{{ 'budgetForm.pickCustomerErr' | translate }}</mat-error>
            }
            @if (emailForm.get('recipient')?.hasError('email')) {
              <mat-error>{{ 'auth.login.emailInvalid' | translate }}</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'budgetShare.subject' | translate }}</mat-label>
            <input matInput formControlName="subject" maxlength="200" />
            @if (emailForm.get('subject')?.hasError('required') && emailForm.get('subject')?.touched) {
              <mat-error>{{ 'budgetShare.subject' | translate }} {{ 'budgetForm.nameRequired' | translate }}</mat-error>
            }
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>{{ 'budgetShare.message' | translate }}</mat-label>
            <textarea matInput formControlName="message" maxlength="5000" rows="5"></textarea>
            @if (emailForm.get('message')?.hasError('required') && emailForm.get('message')?.touched) {
              <mat-error>{{ 'budgetShare.message' | translate }} {{ 'budgetForm.nameRequired' | translate }}</mat-error>
            }
          </mat-form-field>
          <button mat-raised-button color="primary" type="submit"
            [disabled]="loading || emailForm.invalid">
            @if (loading) { <mat-icon class="spin">sync</mat-icon> }
            {{ 'budgetShare.sendEmail' | translate }}
          </button>
        </form>
      }
    </section>
    }
  `,
  styles: [`
    .send-section { display: grid; gap: var(--app-space-md); }
    h2 { margin: 0 0 4px; font-size: 1.15rem; font-weight: 600; }
    .sent-state { margin: 0; display: flex; align-items: center; gap: 6px; color: var(--app-text-secondary); font-size: 0.875rem; }
    .channel-icon { font-size: 18px; width: 18px; height: 18px; }
    .send-buttons { display: flex; flex-wrap: wrap; gap: var(--app-space-sm); }
    .secondary-buttons { display: flex; flex-wrap: wrap; gap: var(--app-space-sm); }
    .wa-icon { color: #25D366 !important; }
    .full-width { width: 100%; }
    .email-form { display: grid; gap: var(--app-space-sm); padding-top: var(--app-space-sm); }
    .spin { animation: spin 1s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
    @media (max-width: 768px) {
      .send-buttons, .secondary-buttons { flex-direction: column; }
      .send-buttons button, .secondary-buttons button { width: 100%; justify-content: center; }
    }
  `],
})
export class EnviarPresupuestoComponent implements OnChanges {
  private readonly translate = inject(TranslateService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  @Input({ required: true }) presupuesto!: Presupuesto;
  @Input()
  set mostrarOpcionesAlInicio(value: boolean) {
    this._mostrarOpcionesAlInicio = value;
    if (value) this.expanded = true;
  }
  get mostrarOpcionesAlInicio(): boolean {
    return this._mostrarOpcionesAlInicio;
  }
  private _mostrarOpcionesAlInicio = false;
  @Output() enviado = new EventEmitter<Presupuesto>();
  expanded = false;
  loading = false;
  mostrarEmail = false;
  telefono = '';
  guardarTelefono = true;
  fallbackPendiente = false;
  waUrl = '';
  email = '';
  asunto = '';
  mensaje = '';
  publicLink = '';
  emailForm!: FormGroup;

  constructor(
    private readonly presupuestos: PresupuestoService,
    private readonly clientes: ClienteService,
    private readonly auth: AuthService,
  ) {}

  ngOnChanges(): void {
    if (this._mostrarOpcionesAlInicio) this.expanded = true;
    if (!this.presupuesto) return;
    this.telefono = this.presupuesto.clienteTelefono ?? '';
    this.email = this.presupuesto.clienteEmail ?? '';
    this.asunto = `Presupuesto - ${this.presupuesto.id}`;
    this.mensaje = construirMensajePresupuesto(this.presupuesto, this.auth.user()?.nombre ?? '');
    this.waUrl = '';
    this.fallbackPendiente = false;
    this.publicLink = '';
    this.emailForm = this.fb.group({
      recipient: [this.email, [Validators.required, Validators.email]],
      subject: [this.asunto, [Validators.required]],
      message: [this.mensaje, [Validators.required]],
    });
  }

  get telefonoNoGuardado(): boolean { return !this.presupuesto?.clienteTelefono?.trim(); }

  async compartirWhatsApp(): Promise<void> {
    const phone = normalizarTelefonoInternacional(this.telefono, this.presupuesto.clientePais || 'ES');
    if (!phone) { this.snackBar.open(this.translate.instant('budgetShare.phoneRequired'), this.translate.instant('common.close'), { duration: 3000 }); return; }
    if (this.telefonoNoGuardado && this.guardarTelefono) await this.guardarTelefonoCliente();
    const publicLink = await this.ensurePublicLink();
    if (!publicLink) return;
    const shareText = `${this.mensaje}\n\n${publicLink}`;
    this.loading = true;
    this.presupuestos.downloadPdf(this.presupuesto.id).subscribe({
      next: blob => {
        this.loading = false;
        const file = new File([blob], `Presupuesto-${this.presupuesto.id}.pdf`, { type: 'application/pdf' });
        const shareData: ShareData = { files: [file], text: shareText };
        if (typeof navigator.share === 'function' && typeof navigator.canShare === 'function' && navigator.canShare(shareData)) {
          void navigator.share(shareData).then(() => this.registrarEnvio('WHATSAPP')).catch((error: unknown) => {
            if ((error as { name?: string })?.name !== 'AbortError') this.snackBar.open(this.translate.instant('budgetShare.shareFailed'), this.translate.instant('common.close'), { duration: 3000 });
          });
          return;
        }
        this.waUrl = `https://wa.me/${phone}?text=${encodeURIComponent(shareText)}`;
        this.fallbackPendiente = true;
        this.saveBlob(file);
        this.snackBar.open(this.translate.instant('budgetShare.fallbackHint'), this.translate.instant('common.close'), { duration: 5000 });
      },
      error: () => { this.loading = false; this.snackBar.open(this.translate.instant('budgetShare.pdfFailed'), this.translate.instant('common.close'), { duration: 3000 }); },
    });
  }

  prepararFallback(): void { this.fallbackPendiente = true; }

  confirmarEnviado(): void { this.registrarEnvio('WHATSAPP'); }

  copiarMensaje(): void {
    void navigator.clipboard?.writeText(this.mensaje).then(() =>
      this.snackBar.open(this.translate.instant('budgetShare.copied'), this.translate.instant('common.close'), { duration: 3000 }));
  }

  descargarPdf(): void {
    this.loading = true;
    this.presupuestos.downloadPdf(this.presupuesto.id).subscribe({
      next: blob => { this.loading = false; this.saveBlob(new File([blob], `Presupuesto-${this.presupuesto.id}.pdf`, { type: 'application/pdf' })); },
      error: () => { this.loading = false; this.snackBar.open(this.translate.instant('budgetShare.pdfFailed'), this.translate.instant('common.close'), { duration: 3000 }); },
    });
  }

  async enviarEmail(): Promise<void> {
    if (this.emailForm.invalid) return;
    const { recipient, subject, message } = this.emailForm.value;
    if (!recipient || !subject || !message || /[\r\n]/.test(subject)) return;
    const publicLink = await this.ensurePublicLink();
    if (!publicLink) return;
    this.loading = true;
    const body = `${message}\n\n${publicLink}`;
    try {
      await firstValueFrom(this.presupuestos.enviarPorEmail(this.presupuesto.id, {
        email: recipient.trim(), asunto: subject.trim(), mensaje: body,
      }));
      this.loading = false;
      this.snackBar.open(this.translate.instant('budgetShare.emailQueued'), this.translate.instant('common.close'), { duration: 3000 });
      this.registrarEnvio('EMAIL', false, false);
    } catch {
      this.loading = false;
      this.snackBar.open(this.translate.instant('budgetShare.emailFailed'), this.translate.instant('common.close'), { duration: 3000 });
    }
  }

  private async ensurePublicLink(force = false): Promise<string | null> {
    if (this.publicLink && !force) return this.publicLink;
    try {
      const created = await firstValueFrom(this.presupuestos.crearEnlace(this.presupuesto.id));
      this.publicLink = created.url;
      return created.url;
    } catch {
      this.snackBar.open(this.translate.instant('publicBudget.linkFailed'), this.translate.instant('common.close'), { duration: 3000 });
      return null;
    }
  }

  private async guardarTelefonoCliente(): Promise<void> {
    try {
      const client = await this.clientes.getById(this.presupuesto.clienteId).toPromise();
      if (client) await this.clientes.update(client.id, { ...client, telefono: this.telefono.trim() }).toPromise();
    } catch { this.snackBar.open(this.translate.instant('budgetShare.phoneSaveFailed'), this.translate.instant('common.close'), { duration: 3000 }); }
  }

  private registrarEnvio(canal: 'WHATSAPP' | 'EMAIL', announce = true, persist = true): void {
    if (!persist) {
      this.actualizarEnvioLocal(canal, announce);
      return;
    }
    this.presupuestos.marcarEnviado(this.presupuesto.id, canal).subscribe({
      next: () => {
        this.actualizarEnvioLocal(canal, announce);
        this.fallbackPendiente = false;
      },
      error: () => { this.snackBar.open(this.translate.instant('budgetShare.markFailed'), this.translate.instant('common.close'), { duration: 3000 }); },
    });
  }

  private actualizarEnvioLocal(canal: 'WHATSAPP' | 'EMAIL', announce: boolean): void {
    this.presupuesto = { ...this.presupuesto, canalEnvio: canal, enviadoAt: new Date().toISOString() };
    if (announce) this.snackBar.open(this.translate.instant('budgetShare.sent'), this.translate.instant('common.close'), { duration: 3000 });
    this.enviado.emit(this.presupuesto);
    this.fallbackPendiente = false;
  }

  private saveBlob(file: File): void {
    const url = URL.createObjectURL(file);
    const anchor = document.createElement('a');
    anchor.href = url; anchor.download = file.name; anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 30_000);
  }
}
