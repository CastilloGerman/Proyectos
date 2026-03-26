import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
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
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email</mat-label>
                <input matInput formControlName="email" type="email" placeholder="tu@email.com">
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

    .forgot-form .full-width {
      width: 100%;
      display: block;
      margin-bottom: 8px;
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
    private snackBar: MatSnackBar
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
        this.sent = true;
      },
    });
  }
}
