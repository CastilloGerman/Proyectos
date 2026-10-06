import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Presupuesto } from '../../../core/models/presupuesto.model';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { AuthService } from '../../../core/auth/auth.service';

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
  imports: [CommonModule, FormsModule, TranslateModule],
  template: `
    <div class="share-actions">
      <button class="share-trigger" type="button" (click)="expanded = !expanded" [attr.aria-expanded]="expanded">
        {{ 'invite.shareTooltip' | translate }}
      </button>
    </div>
    @if (expanded) {
    <section class="share-panel" aria-labelledby="share-title">
      <h2 id="share-title">{{ 'budgetShare.title' | translate }}</h2>
      @if (presupuesto.enviadoAt) {
        <p class="sent-state">{{ 'budgetShare.sent' | translate }} · {{ presupuesto.enviadoAt | date:'short' }} ({{ presupuesto.canalEnvio }})</p>
      }
      @if (telefonoNoGuardado) {
        <label class="field">{{ 'budgetShare.phone' | translate }}
          <input type="tel" [(ngModel)]="telefono" autocomplete="tel" />
        </label>
        <label class="save-phone"><input type="checkbox" [(ngModel)]="guardarTelefono" /> {{ 'budgetShare.savePhone' | translate }}</label>
      }
      <button class="primary" type="button" [disabled]="loading" (click)="compartirWhatsApp()">{{ 'budgetShare.whatsapp' | translate }}</button>
      <button class="secondary" type="button" [disabled]="loading" (click)="mostrarEmail = !mostrarEmail">{{ 'budgetShare.email' | translate }}</button>
      @if (waUrl) {
        <a class="tertiary" [href]="waUrl" target="_blank" rel="noopener" (click)="prepararFallback()">{{ 'budgetShare.openWhatsApp' | translate }}</a>
        @if (fallbackPendiente) { <button class="tertiary" type="button" (click)="confirmarEnviado()">{{ 'budgetShare.alreadySent' | translate }}</button> }
      }
      <button class="tertiary" type="button" (click)="copiarMensaje()">{{ 'budgetShare.copy' | translate }}</button>
      <button class="tertiary" type="button" [disabled]="loading" (click)="descargarPdf()">{{ 'budgetShare.download' | translate }}</button>
      @if (mostrarEmail) {
        <form class="email-form" (ngSubmit)="enviarEmail()">
          <label class="field">{{ 'budgetShare.recipient' | translate }}<input type="email" name="recipient" [(ngModel)]="email" required maxlength="254" /></label>
          <label class="field">{{ 'budgetShare.subject' | translate }}<input name="subject" [(ngModel)]="asunto" required maxlength="200" /></label>
          <label class="field">{{ 'budgetShare.message' | translate }}<textarea name="message" [(ngModel)]="mensaje" required maxlength="5000" rows="5"></textarea></label>
          <button class="primary email-submit" type="submit" [disabled]="loading || !email.trim() || !asunto.trim() || !mensaje.trim()">{{ 'budgetShare.sendEmail' | translate }}</button>
        </form>
      }
      @if (feedback) { <p role="status" class="feedback">{{ feedback }}</p> }
    </section>
    }
  `,
  styles: [`
    .share-actions { margin-top:20px; }
    .share-trigger { min-height:44px; padding:8px 18px; border:0; border-radius:8px; background:var(--app-primary,#2563eb); color:#fff; font:inherit; font-weight:600; cursor:pointer; }
    .share-panel { display:grid; gap:12px; padding:16px; margin:12px 0 0; border:1px solid var(--app-border,#d1d5db); border-radius:12px; }
    h2 { margin:0 0 4px; font-size:1.15rem; }
    button { min-height:48px; padding:10px 16px; border-radius:8px; font:inherit; cursor:pointer; }
    button:disabled { opacity:.55; cursor:wait; }
    .primary { color:#fff; background:#128c4a; border:0; font-weight:700; }
    .secondary { background:transparent; border:1px solid currentColor; font-weight:600; }
    .tertiary { min-height:44px; border:0; background:transparent; text-align:left; color:inherit; text-decoration:underline; }
    .field { display:grid; gap:6px; font-weight:600; }
    input,textarea { box-sizing:border-box; width:100%; min-height:44px; padding:10px; border:1px solid #87909d; border-radius:6px; font:inherit; }
    textarea { resize:vertical; }
    .save-phone { display:flex; align-items:center; gap:8px; }
    .save-phone input { width:22px; min-height:22px; }
    .email-form { display:grid; gap:12px; padding-top:8px; }
    .feedback, .sent-state { margin:0; }
  `],
})
export class EnviarPresupuestoComponent implements OnChanges {
  @Input({ required: true }) presupuesto!: Presupuesto;
  @Input() mostrarOpcionesAlInicio = false;
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
  feedback = '';

  constructor(
    private readonly presupuestos: PresupuestoService,
    private readonly clientes: ClienteService,
    private readonly auth: AuthService,
    private readonly translate: TranslateService,
  ) {}

  ngOnChanges(): void {
    if (this.mostrarOpcionesAlInicio) this.expanded = true;
    if (!this.presupuesto) return;
    this.telefono = this.presupuesto.clienteTelefono ?? '';
    this.email = this.presupuesto.clienteEmail ?? '';
    this.asunto = `Presupuesto - ${this.presupuesto.id}`;
    this.mensaje = construirMensajePresupuesto(this.presupuesto, this.auth.user()?.nombre ?? '');
    this.waUrl = '';
    this.fallbackPendiente = false;
  }

  get telefonoNoGuardado(): boolean { return !this.presupuesto?.clienteTelefono?.trim(); }

  async compartirWhatsApp(): Promise<void> {
    const phone = normalizarTelefonoInternacional(this.telefono, this.presupuesto.clientePais || 'ES');
    if (!phone) { this.feedback = this.translate.instant('budgetShare.phoneRequired'); return; }
    if (this.telefonoNoGuardado && this.guardarTelefono) await this.guardarTelefonoCliente();
    this.loading = true;
    this.presupuestos.downloadPdf(this.presupuesto.id).subscribe({
      next: blob => {
        this.loading = false;
        const file = new File([blob], `Presupuesto-${this.presupuesto.id}.pdf`, { type: 'application/pdf' });
        const shareData: ShareData = { files: [file], text: this.mensaje };
        if (typeof navigator.share === 'function' && typeof navigator.canShare === 'function' && navigator.canShare(shareData)) {
          void navigator.share(shareData).then(() => this.registrarEnvio('WHATSAPP')).catch((error: unknown) => {
            if ((error as { name?: string })?.name !== 'AbortError') this.feedback = this.translate.instant('budgetShare.shareFailed');
          });
          return;
        }
        this.waUrl = `https://wa.me/${phone}?text=${encodeURIComponent(this.mensaje)}`;
        this.fallbackPendiente = true;
        this.saveBlob(file);
        this.feedback = this.translate.instant('budgetShare.fallbackHint');
      },
      error: () => { this.loading = false; this.feedback = this.translate.instant('budgetShare.pdfFailed'); },
    });
  }

  prepararFallback(): void { this.fallbackPendiente = true; }

  confirmarEnviado(): void { this.registrarEnvio('WHATSAPP'); }

  copiarMensaje(): void {
    void navigator.clipboard?.writeText(this.mensaje).then(() => this.feedback = this.translate.instant('budgetShare.copied'));
  }

  descargarPdf(): void {
    this.loading = true;
    this.presupuestos.downloadPdf(this.presupuesto.id).subscribe({
      next: blob => { this.loading = false; this.saveBlob(new File([blob], `Presupuesto-${this.presupuesto.id}.pdf`, { type: 'application/pdf' })); },
      error: () => { this.loading = false; this.feedback = this.translate.instant('budgetShare.pdfFailed'); },
    });
  }

  enviarEmail(): void {
    if (!this.email.trim() || !this.asunto.trim() || !this.mensaje.trim() || /[\r\n]/.test(this.asunto)) return;
    this.loading = true;
    this.presupuestos.enviarPorEmail(this.presupuesto.id, { email: this.email.trim(), asunto: this.asunto.trim(), mensaje: this.mensaje }).subscribe({
      next: () => { this.loading = false; this.feedback = this.translate.instant('budgetShare.emailQueued'); this.registrarEnvio('EMAIL', false, false); },
      error: () => { this.loading = false; this.feedback = this.translate.instant('budgetShare.emailFailed'); },
    });
  }

  private async guardarTelefonoCliente(): Promise<void> {
    try {
      const client = await this.clientes.getById(this.presupuesto.clienteId).toPromise();
      if (client) await this.clientes.update(client.id, { ...client, telefono: this.telefono.trim() }).toPromise();
    } catch { this.feedback = this.translate.instant('budgetShare.phoneSaveFailed'); }
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
      error: () => { this.feedback = this.translate.instant('budgetShare.markFailed'); },
    });
  }

  private actualizarEnvioLocal(canal: 'WHATSAPP' | 'EMAIL', announce: boolean): void {
    this.presupuesto = { ...this.presupuesto, canalEnvio: canal, enviadoAt: new Date().toISOString() };
    if (announce) this.feedback = this.translate.instant('budgetShare.sent');
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
