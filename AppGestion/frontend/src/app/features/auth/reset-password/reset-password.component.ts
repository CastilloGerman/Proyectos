import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';

function passwordMatchValidator(g: AbstractControl): ValidationErrors | null {
  const a = g.get('newPassword')?.value;
  const b = g.get('confirmPassword')?.value;
  if (a && b && a !== b) return { passwordMismatch: true };
  return null;
}

@Component({
    selector: 'app-reset-password',
    imports: [
        ReactiveFormsModule,
        RouterLink,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatProgressSpinnerModule,
    ],
    template: `
    <div class="reset-wrapper">
      <div class="reset-container">
        <div class="reset-card">
          <div class="reset-logo">
            <img src="assets/noemi-logo.png" alt="Noemí" class="reset-logo-img" />
          </div>
          <h1 class="reset-title">Nueva contraseña</h1>
          <p class="reset-subtitle">Introduce tu nueva contraseña (mínimo 12 caracteres).</p>
          @if (token) {
            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="reset-form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nueva contraseña</mat-label>
                <input matInput formControlName="newPassword" type="password" autocomplete="new-password">
                @if (form.get('newPassword')?.hasError('required') && form.get('newPassword')?.touched) {
                  <mat-error>La contraseña es obligatoria</mat-error>
                }
                @if (form.get('newPassword')?.hasError('minlength')) {
                  <mat-error>Mínimo 12 caracteres</mat-error>
                }
              </mat-form-field>
              @if (needsTotp) {
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Código de verificación (2FA)</mat-label>
                  <input matInput formControlName="totpCode" type="text" inputmode="numeric"
                         autocomplete="one-time-code" maxlength="10">
                  <mat-hint>Introduce el código de tu app de autenticación</mat-hint>
                  @if (form.get('totpCode')?.hasError('required') && form.get('totpCode')?.touched) {
                    <mat-error>El código es obligatorio</mat-error>
                  }
                </mat-form-field>
              }
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Repetir contraseña</mat-label>
                <input matInput formControlName="confirmPassword" type="password" autocomplete="new-password">
                @if (form.get('confirmPassword')?.touched && form.hasError('passwordMismatch')) {
                  <mat-error>Las contraseñas no coinciden</mat-error>
                }
              </mat-form-field>
              <button mat-raised-button color="primary" type="submit" [disabled]="loading" class="submit-btn">
                @if (loading) {
                  <mat-spinner diameter="24"></mat-spinner>
                } @else {
                  Guardar contraseña
                }
              </button>
            </form>
          } @else {
            <p class="reset-error">Enlace inválido o expirado. Solicita de nuevo el restablecimiento de contraseña.</p>
            <a mat-raised-button color="primary" routerLink="/forgot-password" class="submit-btn">Solicitar enlace de nuevo</a>
          }
          <a routerLink="/login" class="back-link">Volver al inicio de sesión</a>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .reset-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: linear-gradient(160deg, #e8ecf4 0%, #d4dceb 40%, #c9d1e0 100%);
    }

    .reset-container {
      width: 100%;
      max-width: 420px;
    }

    .reset-card {
      background: rgba(255, 255, 255, 0.95);
      border-radius: 28px;
      padding: 40px 36px;
      box-shadow: 0 20px 60px rgba(30, 58, 138, 0.08), 0 8px 24px rgba(0, 0, 0, 0.04);
    }

    .reset-logo {
      text-align: center;
      margin-bottom: 24px;
    }

    .reset-logo-img {
      height: 72px;
      width: auto;
      object-fit: contain;
    }

    .reset-title {
      margin: 0 0 4px 0;
      font-size: 1.5rem;
      font-weight: 600;
      color: #1e293b;
      text-align: center;
    }

    .reset-subtitle {
      margin: 0 0 24px 0;
      font-size: 0.9rem;
      color: #64748b;
      text-align: center;
      line-height: 1.4;
    }

    .reset-form .full-width {
      width: 100%;
      display: block;
      margin-bottom: 8px;
    }

    /*
     * Sobre tarjeta clara: forzar tokens del outlined field. Si html tiene tema oscuro,
     * Material define bordes/etiquetas claros (pensados para fondo oscuro) y el borde casi
     * desaparece sobre blanco hasta el foco.
     */
    .reset-card .reset-form .mat-mdc-form-field {
      --mdc-outlined-text-field-outline-color: rgba(51, 65, 85, 0.45);
      --mdc-outlined-text-field-hover-outline-color: rgba(30, 58, 138, 0.55);
      --mdc-outlined-text-field-focus-outline-color: #39488e;
      --mdc-outlined-text-field-input-text-color: #0f172a;
      --mdc-outlined-text-field-label-text-color: #64748b;
      --mdc-outlined-text-field-hover-label-text-color: #475569;
      --mdc-outlined-text-field-focus-label-text-color: #39488e;
      --mat-form-field-outlined-label-text-color: #64748b;
      --mat-form-field-outlined-label-text-populated-color: #64748b;
      --mat-form-field-error-text-color: #b91c1c;
      --mat-form-field-hint-text-color: #64748b;
    }

    .reset-form .full-width ::ng-deep .mat-mdc-form-field-subscript-wrapper {
      margin-bottom: 8px;
    }

    .reset-form .full-width ::ng-deep .mat-mdc-text-field-wrapper {
      border-radius: 14px;
    }

    .reset-form .full-width ::ng-deep .mdc-notched-outline__notch {
      min-width: 64px;
    }

    :host-context(html.app-dark-theme) .reset-wrapper {
      background: linear-gradient(165deg, #0c1220 0%, #141b2d 45%, #0f1419 100%);
    }

    :host-context(html.app-dark-theme) .reset-card {
      background: var(--app-bg-card, #1a222c);
      border: 1px solid var(--app-border, rgba(255, 255, 255, 0.1));
      box-shadow: var(--app-shadow-lg, 0 12px 40px rgba(0, 0, 0, 0.5));
      color: var(--app-text-primary, #f8fafc);
    }

    :host-context(html.app-dark-theme) .reset-title {
      color: var(--app-text-primary, #f8fafc);
    }

    :host-context(html.app-dark-theme) .reset-subtitle {
      color: var(--app-text-secondary, #cbd5e1);
    }

    :host-context(html.app-dark-theme) .reset-card .reset-form .mat-mdc-form-field {
      --mdc-outlined-text-field-input-text-color: var(--app-text-primary, #f8fafc);
      --mdc-outlined-text-field-label-text-color: var(--app-text-secondary, #cbd5e1);
      --mdc-outlined-text-field-hover-label-text-color: var(--app-text-primary, #f8fafc);
      --mdc-outlined-text-field-focus-label-text-color: var(--app-text-secondary, #cbd5e1);
      --mdc-outlined-text-field-outline-color: rgba(148, 163, 184, 0.55);
      --mdc-outlined-text-field-hover-outline-color: rgba(203, 213, 225, 0.75);
      --mdc-outlined-text-field-focus-outline-color: #818cf8;
      --mat-form-field-outlined-label-text-color: var(--app-text-secondary, #cbd5e1);
      --mat-form-field-outlined-label-text-populated-color: var(--app-text-secondary, #cbd5e1);
      --mat-form-field-error-text-color: #fca5a5;
      --mat-form-field-hint-text-color: var(--app-text-muted, #94a3b8);
    }

    :host-context(html.app-dark-theme) .reset-error {
      background: rgba(248, 113, 113, 0.12);
      color: #fca5a5;
    }

    :host-context(html.app-dark-theme) .back-link {
      color: var(--app-text-secondary, #cbd5e1);
    }

    :host-context(html.app-dark-theme) .back-link:hover {
      color: #a5b4fc;
    }

    .submit-btn {
      width: 100%;
      margin-top: 16px;
      padding: 12px 24px;
      font-size: 1rem;
      border-radius: 14px;
    }

    .reset-error {
      margin: 0 0 24px 0;
      padding: 16px;
      background: #fef2f2;
      border-radius: 12px;
      color: #b91c1c;
      font-size: 0.9rem;
      line-height: 1.5;
    }

    .back-link {
      display: block;
      text-align: center;
      padding-top: 20px;
      color: #64748b;
      text-decoration: none;
      font-size: 0.9rem;
    }

    .back-link:hover {
      color: #1e3a8a;
    }

    mat-spinner {
      display: inline-block;
      vertical-align: middle;
      margin-right: 8px;
    }
  `]
})
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  token: string | null = null;
  loading = false;
  /** SEC-09: el backend pide el código 2FA si la cuenta lo tiene activado (error TOTP_REQUERIDO). */
  needsTotp = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
    private snackBar: MatSnackBar,
    private translate: TranslateService
  ) {
    this.form = this.fb.group(
      {
        newPassword: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(128)]],
        confirmPassword: ['', Validators.required],
        totpCode: [''],
      },
      { validators: passwordMatchValidator }
    );
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
  }

  onSubmit(): void {
    if (this.form.invalid || !this.token) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    const totp = this.needsTotp ? String(this.form.value.totpCode ?? '').trim() : undefined;
    this.auth.resetPassword(this.token, this.form.value.newPassword, totp).subscribe({
      next: () => {
        this.snackBar.open(
          this.translate.instant('auth.passwordReset.success'),
          this.translate.instant('common.close'),
          { duration: 5000 },
        );
        this.router.navigate(['/login']);
      },
      error: (err: unknown) => {
        this.loading = false;
        const apiErr = err as { error?: { message?: string } };
        const srv =
          typeof apiErr.error?.message === 'string' ? String(apiErr.error.message).trim() : '';
        if (srv === 'TOTP_REQUERIDO') {
          this.needsTotp = true;
          this.form.get('totpCode')?.setValidators([Validators.required]);
          this.form.get('totpCode')?.updateValueAndValidity();
          return;
        }
        const msg = srv.length > 0 ? srv : this.translate.instant('auth.passwordReset.invalidLink');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }
}
