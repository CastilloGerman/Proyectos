import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
    selector: 'app-forgot-password',
    imports: [
        ReactiveFormsModule,
        RouterLink,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatProgressSpinnerModule,
    ],
    template: `
    <div class="forgot-wrapper">
      <div class="forgot-container">
        <div class="forgot-card">
          <div class="forgot-logo">
            <img src="assets/noemi-logo.png" alt="Noemí" class="forgot-logo-img" />
          </div>
          <h1 class="forgot-title">Recuperar contraseña</h1>
          <p class="forgot-subtitle">Indica tu email y te enviaremos un enlace para restablecer la contraseña.</p>
          @if (!sent) {
            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="forgot-form">
              <mat-form-field appearance="outline" floatLabel="always" class="full-width">
                <mat-label>Email</mat-label>
                <input matInput formControlName="email" type="email" placeholder="tu@email.com" autocomplete="email">
                @if (form.get('email')?.hasError('required') && form.get('email')?.touched) {
                  <mat-error>El email es obligatorio</mat-error>
                }
                @if (form.get('email')?.hasError('email')) {
                  <mat-error>Email inválido</mat-error>
                }
              </mat-form-field>
              <button mat-raised-button color="primary" type="submit" [disabled]="loading" class="submit-btn">
                @if (loading) {
                  <mat-spinner diameter="24"></mat-spinner>
                } @else {
                  Enviar enlace
                }
              </button>
            </form>
          } @else {
            <p class="forgot-success">Si el email está registrado, recibirás un enlace para restablecer tu contraseña. Revisa tu bandeja de entrada y la carpeta de spam.</p>
          }
          <a routerLink="/login" class="back-link">Volver al inicio de sesión</a>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .forgot-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: linear-gradient(160deg, #e8ecf4 0%, #d4dceb 40%, #c9d1e0 100%);
    }

    .forgot-container {
      width: 100%;
      max-width: 420px;
    }

    .forgot-card {
      background: rgba(255, 255, 255, 0.95);
      border-radius: 28px;
      padding: 40px 36px;
      box-shadow: 0 20px 60px rgba(30, 58, 138, 0.08), 0 8px 24px rgba(0, 0, 0, 0.04);
    }

    .forgot-logo {
      text-align: center;
      margin-bottom: 24px;
    }

    .forgot-logo-img {
      height: 72px;
      width: auto;
      object-fit: contain;
    }

    .forgot-title {
      margin: 0 0 4px 0;
      font-size: 1.5rem;
      font-weight: 600;
      color: #1e293b;
      text-align: center;
    }

    .forgot-subtitle {
      margin: 0 0 24px 0;
      font-size: 0.9rem;
      color: #64748b;
      text-align: center;
      line-height: 1.4;
    }

    .forgot-form {
      display: block;
    }

    .forgot-form .full-width {
      width: 100%;
      display: block;
      margin-bottom: 8px;
    }

    /*
     * Sobre tarjeta clara: forzar tokens del outlined field. Si html tiene tema oscuro,
     * Material define bordes/etiquetas claros (pensados para fondo oscuro) y el borde casi
     * desaparece sobre blanco hasta el foco.
     */
    .forgot-card .forgot-form .mat-mdc-form-field {
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
    }

    .forgot-form .full-width ::ng-deep .mat-mdc-form-field-subscript-wrapper {
      margin-bottom: 8px;
    }

    .forgot-form .full-width ::ng-deep .mat-mdc-text-field-wrapper {
      border-radius: 14px;
    }

    .forgot-form .full-width ::ng-deep .mdc-notched-outline__notch {
      min-width: 64px;
    }

    .forgot-form .full-width ::ng-deep .mdc-notched-outline__leading,
    .forgot-form .full-width ::ng-deep .mdc-notched-outline__notch,
    .forgot-form .full-width ::ng-deep .mdc-notched-outline__trailing {
      transition: border-color 0.25s ease, box-shadow 0.25s ease;
    }

    .forgot-card .forgot-form input::placeholder {
      color: #94a3b8;
      opacity: 1;
    }

    :host-context(html.app-dark-theme) .forgot-wrapper {
      background: linear-gradient(165deg, #0c1220 0%, #141b2d 45%, #0f1419 100%);
    }

    :host-context(html.app-dark-theme) .forgot-card {
      background: var(--app-bg-card, #1a222c);
      border: 1px solid var(--app-border, rgba(255, 255, 255, 0.1));
      box-shadow: var(--app-shadow-lg, 0 12px 40px rgba(0, 0, 0, 0.5));
      color: var(--app-text-primary, #f8fafc);
    }

    :host-context(html.app-dark-theme) .forgot-title {
      color: var(--app-text-primary, #f8fafc);
    }

    :host-context(html.app-dark-theme) .forgot-subtitle {
      color: var(--app-text-secondary, #cbd5e1);
    }

    :host-context(html.app-dark-theme) .forgot-card .forgot-form .mat-mdc-form-field {
      --mdc-outlined-text-field-input-text-color: var(--app-text-primary, #f8fafc);
      --mdc-outlined-text-field-label-text-color: var(--app-text-secondary, #cbd5e1);
      --mdc-outlined-text-field-hover-label-text-color: var(--app-text-primary, #f8fafc);
      --mdc-outlined-text-field-focus-label-text-color: var(--app-text-secondary, #cbd5e1);
      --mdc-outlined-text-field-outline-color: var(--app-border, rgba(255, 255, 255, 0.22));
      --mdc-outlined-text-field-hover-outline-color: rgba(148, 163, 184, 0.45);
      --mdc-outlined-text-field-focus-outline-color: #818cf8;
      --mat-form-field-outlined-label-text-color: var(--app-text-secondary, #cbd5e1);
      --mat-form-field-outlined-label-text-populated-color: var(--app-text-secondary, #cbd5e1);
      --mat-form-field-error-text-color: #fca5a5;
    }

    :host-context(html.app-dark-theme) .forgot-card .forgot-form input::placeholder {
      color: var(--app-text-muted, #94a3b8);
      opacity: 1;
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

    .forgot-success {
      margin: 0 0 24px 0;
      padding: 16px;
      background: #f0fdf4;
      border-radius: 12px;
      color: #166534;
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

    :host-context(html.app-dark-theme) .forgot-success {
      background: rgba(34, 197, 94, 0.12);
      color: #86efac;
    }

    mat-spinner {
      display: inline-block;
      vertical-align: middle;
      margin-right: 8px;
    }
  `]
})
export class ForgotPasswordComponent {
  form: FormGroup;
  loading = false;
  sent = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private snackBar: MatSnackBar,
    private translate: TranslateService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.auth.forgotPassword(this.form.value.email).subscribe({
      next: () => {
        this.loading = false;
        this.sent = true;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open(this.translate.instant('auth.forgot.requestError'), this.translate.instant('common.close'), {
          duration: 7000,
        });
      },
    });
  }
}
