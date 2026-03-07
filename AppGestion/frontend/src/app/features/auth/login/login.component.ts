import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="login-wrapper">
      <div class="login-container">
        <div class="login-card">
          <div class="login-logo">
            <img src="assets/noemi-logo.png" alt="Noemí" class="login-logo-img" />
          </div>
          <h1 class="login-title">Iniciar sesión</h1>
          <p class="login-subtitle">Accede a tu cuenta</p>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="login-form">
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
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Contraseña</mat-label>
              <input matInput formControlName="password" type="password">
              @if (form.get('password')?.hasError('required') && form.get('password')?.touched) {
                <mat-error>La contraseña es obligatoria</mat-error>
              }
            </mat-form-field>
            <button mat-raised-button color="primary" type="submit" [disabled]="loading" class="submit-btn">
              @if (loading) {
                <mat-spinner diameter="24"></mat-spinner>
              } @else {
                Entrar
              }
            </button>
          </form>
          <a routerLink="/register" class="register-link">¿No tienes cuenta? Regístrate</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: linear-gradient(160deg, #e8ecf4 0%, #d4dceb 40%, #c9d1e0 100%);
      transition: background 0.4s ease;
    }

    .login-container {
      width: 100%;
      max-width: 420px;
      animation: loginFadeIn 0.5s ease-out;
    }

    @keyframes loginFadeIn {
      from {
        opacity: 0;
        transform: translateY(16px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .login-card {
      background: rgba(255, 255, 255, 0.95);
      border-radius: 28px;
      padding: 40px 36px;
      box-shadow: 0 20px 60px rgba(30, 58, 138, 0.08), 0 8px 24px rgba(0, 0, 0, 0.04);
      transition: box-shadow 0.3s ease, transform 0.3s ease;
    }

    .login-card:hover {
      box-shadow: 0 24px 72px rgba(30, 58, 138, 0.1), 0 12px 32px rgba(0, 0, 0, 0.06);
    }

    .login-logo {
      text-align: center;
      margin-bottom: 24px;
    }

    .login-logo-img {
      height: 96px;
      width: auto;
      object-fit: contain;
    }

    .login-title {
      margin: 0 0 4px 0;
      font-size: 1.6rem;
      font-weight: 600;
      color: #1e293b;
      text-align: center;
      letter-spacing: -0.02em;
    }

    .login-subtitle {
      margin: 0 0 28px 0;
      font-size: 0.95rem;
      color: #64748b;
      text-align: center;
    }

    .login-form {
      display: block;
    }

    .full-width {
      width: 100%;
      display: block;
      margin-bottom: 8px;
    }

    .full-width ::ng-deep .mat-mdc-form-field-subscript-wrapper {
      margin-bottom: 8px;
    }

    .full-width ::ng-deep .mat-mdc-text-field-wrapper {
      border-radius: 14px;
    }

    .full-width ::ng-deep .mdc-notched-outline__notch {
      min-width: 64px;
    }

    .full-width ::ng-deep .mdc-notched-outline__leading,
    .full-width ::ng-deep .mdc-notched-outline__notch,
    .full-width ::ng-deep .mdc-notched-outline__trailing {
      transition: border-color 0.25s ease, box-shadow 0.25s ease;
    }

    .submit-btn {
      width: 100%;
      margin-top: 16px;
      margin-bottom: 8px;
      padding: 12px 24px;
      font-size: 1rem;
      border-radius: 14px;
      transition: all 0.3s ease;
    }

    .submit-btn:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 8px 24px rgba(30, 58, 138, 0.25);
    }

    .register-link {
      display: block;
      text-align: center;
      padding: 20px 0 0;
      color: #64748b;
      text-decoration: none;
      font-size: 0.9rem;
      transition: color 0.25s ease;
    }

    .register-link:hover {
      color: #1e3a8a;
    }

    mat-spinner {
      display: inline-block;
      vertical-align: middle;
      margin-right: 8px;
    }
  `],
})
export class LoginComponent implements OnInit {
  form: FormGroup;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.auth.login(this.form.value).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(
          err.error?.message || 'Credenciales inválidas',
          'Cerrar',
          { duration: 4000 }
        );
      },
    });
  }
}
