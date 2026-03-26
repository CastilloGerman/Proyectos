import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { Clipboard, ClipboardModule } from '@angular/cdk/clipboard';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-invitar-usuario-dialog',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatDialogModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatSnackBarModule,
        MatIconModule,
        MatTooltipModule,
        MatMenuModule,
        MatDividerModule,
        ClipboardModule,
    ],
    template: `
    <h2 mat-dialog-title class="dialog-title">Enviar enlace de referido</h2>
    <mat-dialog-content>
      <p class="hint">
        Se enviará un correo con un enlace para que esa persona cree <strong>su propia cuenta</strong>
        con periodo de prueba. No comparte tus datos ni un “rol”: para seguir editando después del
        trial necesitará suscripción activa, igual que tú.
      </p>
      <form [formGroup]="form">
        <mat-form-field appearance="outline" class="full">
          <mat-label>Email del referido</mat-label>
          <input matInput type="email" formControlName="email" />
        </mat-form-field>
      </form>

      <mat-divider class="divider"></mat-divider>

      <p class="section-title">Página principal</p>
      <p class="hint section-hint">Comparte este enlace por donde quieras; quien entre podrá registrarse.</p>
      <mat-form-field appearance="outline" class="full link-field">
        <mat-label>Enlace de la aplicación</mat-label>
        <input matInput [value]="paginaPrincipalUrl" readonly tabindex="-1" />
        <div class="suffix-actions" matSuffix>
          <button
            mat-icon-button
            type="button"
            [matMenuTriggerFor]="shareMenu"
            matTooltip="Compartir"
            aria-label="Compartir enlace">
            <mat-icon>share</mat-icon>
          </button>
          <button
            mat-icon-button
            type="button"
            (click)="copiarPaginaPrincipal()"
            matTooltip="Copiar enlace"
            aria-label="Copiar enlace">
            <mat-icon>content_copy</mat-icon>
          </button>
        </div>
      </mat-form-field>
      <mat-menu #shareMenu="matMenu">
        <button mat-menu-item type="button" (click)="compartirWhatsApp()">
          <img src="assets/whatsapp-logo.png" alt="" class="wa-menu-logo" width="22" height="22" />
          <span>WhatsApp</span>
        </button>
        @if (puedeCompartirNativo) {
        <button mat-menu-item type="button" (click)="compartirNativo()">
          <mat-icon>send</mat-icon>
          <span>Compartir (sistema)</span>
        </button>
        }
      </mat-menu>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-raised-button color="primary" (click)="enviar()" [disabled]="form.invalid || sending">
        Enviar enlace
      </button>
    </mat-dialog-actions>
  `,
    styles: [`
    :host {
      display: block;
      color: var(--app-text-primary);
    }
    .full {
      width: 100%;
      --mdc-outlined-text-field-input-text-color: var(--app-text-primary);
      --mdc-outlined-text-field-label-text-color: var(--app-text-secondary);
      --mat-form-field-outlined-label-text-color: var(--app-text-secondary);
    }
    .dialog-title {
      color: var(--app-text-primary);
      font-size: 1.25rem;
      font-weight: 600;
    }
    .hint {
      font-size: 13px;
      color: var(--app-text-secondary);
      margin-top: 0;
      line-height: 1.45;
    }
    .hint strong {
      color: var(--app-text-primary);
      font-weight: 600;
    }
    .section-hint { margin-bottom: 8px; }
    .section-title {
      font-size: 14px;
      font-weight: 600;
      margin: 16px 0 0;
      color: var(--app-text-primary);
    }
    .divider { margin: 16px 0 8px; }
    .link-field ::ng-deep .mat-mdc-form-field-subscript-wrapper { display: none; }
    .suffix-actions {
      display: flex;
      align-items: center;
      margin-right: -4px;
    }
    .wa-menu-logo {
      width: 22px;
      height: 22px;
      object-fit: contain;
      flex-shrink: 0;
      margin-right: 12px;
      vertical-align: middle;
    }
  `]
})
export class InvitarUsuarioDialogComponent {
  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });
  sending = false;

  /** Web Share API (móvil / algunos navegadores). */
  puedeCompartirNativo =
    typeof navigator !== 'undefined' && typeof navigator.share === 'function';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private ref: MatDialogRef<InvitarUsuarioDialogComponent, boolean>,
    private snackBar: MatSnackBar,
    private clipboard: Clipboard
  ) {}

  /** URL pública del front (home). */
  get paginaPrincipalUrl(): string {
    const configured = (environment as { appPublicUrl?: string }).appPublicUrl;
    if (configured && configured.trim().length > 0) {
      return configured.replace(/\/$/, '');
    }
    if (typeof window !== 'undefined' && window.location?.origin) {
      return window.location.origin;
    }
    return 'http://localhost:4200';
  }

  copiarPaginaPrincipal(): void {
    const ok = this.clipboard.copy(this.paginaPrincipalUrl);
    this.snackBar.open(ok ? 'Enlace copiado al portapapeles' : 'No se pudo copiar', 'Cerrar', {
      duration: 2500,
    });
  }

  textoCompartir(): string {
    return `Prueba AppGestion — gestión de presupuestos y facturas: ${this.paginaPrincipalUrl}`;
  }

  compartirWhatsApp(): void {
    const text = encodeURIComponent(this.textoCompartir());
    const url = `https://wa.me/?text=${text}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  async compartirNativo(): Promise<void> {
    try {
      await navigator.share({
        title: 'AppGestion',
        text: this.textoCompartir(),
        url: this.paginaPrincipalUrl,
      });
    } catch {
      this.copiarPaginaPrincipal();
    }
  }

  enviar(): void {
    if (this.form.invalid) return;
    this.sending = true;
    const v = this.form.value;
    this.auth.sendInvitation(v.email!.trim()).subscribe({
      next: () => {
        this.ref.close(true);
        this.sending = false;
      },
      error: (err) => {
        this.sending = false;
        const msg = err.error?.message || err.error?.error || 'No se pudo enviar el enlace';
        this.snackBar.open(msg, 'Cerrar', { duration: 6000 });
      },
    });
  }
}
