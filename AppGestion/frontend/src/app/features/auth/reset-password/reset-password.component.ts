import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/auth/auth.service';

function passwordMatchValidator(g: AbstractControl): ValidationErrors | null {
  const a = g.get('newPassword')?.value;
  const b = g.get('confirmPassword')?.value;
  if (a && b && a !== b) return { passwordMismatch: true };
  return null;
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
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
          <p class="reset-subtitle">Introduce tu nueva contraseña (mínimo 6 caracteres).</p>
          @if (token) {
            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="reset-form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nueva contraseña</mat-label>
                <input matInput formControlName="newPassword" type="password" autocomplete="new-password">
                @if (form.get('newPassword')?.hasError('required') && form.get('newPassword')?.touched) {
                  <mat-error>La contraseña es obligatoria</mat-error>
                }
                @if (form.get('newPassword')?.hasError('minlength')) {
                  <mat-error>Mínimo 6 caracteres</mat-error>
                }
              </mat-form-field>
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
  `],
})
export class ResetPasswordComponent implements OnInit {
  form: FormGroup;
  token: string | null = null;
  loading = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group(
      {
        newPassword: ['', [Validators.required, Validators.minLength(6)]],
        confirmPassword: ['', Validators.required],
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
    this.auth.resetPassword(this.token, this.form.value.newPassword).subscribe({
      next: () => {
        this.snackBar.open('Contraseña actualizada. Ya puedes iniciar sesión.', 'Cerrar', { duration: 5000 });
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(
          err.error?.message || 'Enlace inválido o expirado. Solicita uno nuevo.',
          'Cerrar',
          { duration: 5000 }
        );
      },
    });
  }
}
