import { AfterViewInit, Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/auth/auth.service';

/** Client ID de Google para el botón "Iniciar con Google" (mismo que en environment). */
const DEFAULT_GOOGLE_CLIENT_ID = '622654316729-itkgprp568mrobd3v8lgnah0cfjchog9.apps.googleusercontent.com';

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
          <p class="login-tagline">Tu facturación en 30 segundos</p>
          <p class="login-subtitle">Gestiona clientes, presupuestos y facturas en un solo lugar.</p>
          <h1 class="login-title">Iniciar sesión</h1>
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
          <div class="login-divider">
            <span>o</span>
          </div>
          <div class="google-button-wrap">
            <div #googleButtonRef class="google-button-inner"></div>
            @if (!googleClientId) {
              <button type="button" class="google-fallback-btn" (click)="onGoogleFallback()">
                <span class="google-g-letter">G</span>
                Continuar con Google
              </button>
            }
          </div>
          <a routerLink="/forgot-password" class="forgot-link">¿Olvidaste tu contraseña?</a>
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
      margin: 20px 0 20px 0;
      font-size: 1.6rem;
      font-weight: 600;
      color: #1e293b;
      text-align: center;
      letter-spacing: -0.02em;
    }

    .login-tagline {
      margin: 0 0 4px 0;
      font-size: 1rem;
      font-weight: 600;
      color: #1e3a8a;
      text-align: center;
    }

    .login-subtitle {
      margin: 0 0 24px 0;
      font-size: 0.95rem;
      color: #64748b;
      text-align: center;
    }

    .login-divider {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 20px 0 16px 0;
    }

    .login-divider::before,
    .login-divider::after {
      content: '';
      flex: 1;
      height: 1px;
      background: var(--app-border, rgba(0,0,0,0.08));
    }

    .login-divider span {
      font-size: 0.8rem;
      color: #94a3b8;
    }

    .google-button-wrap {
      display: flex;
      flex-direction: column;
      align-items: center;
      margin-bottom: 8px;
      min-height: 44px;
    }

    .google-button-inner {
      min-height: 44px;
      display: flex;
      justify-content: center;
      align-items: center;
    }

    .google-button-inner ::ng-deep iframe {
      margin: 0;
    }

    .google-fallback-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      width: 100%;
      max-width: 320px;
      padding: 10px 20px;
      font-size: 1rem;
      font-weight: 500;
      color: #3c4043;
      background: #fff;
      border: 1px solid #dadce0;
      border-radius: 8px;
      cursor: pointer;
      transition: background 0.2s, box-shadow 0.2s;
    }

    .google-fallback-btn:hover {
      background: #f8f9fa;
      box-shadow: 0 1px 3px rgba(0,0,0,0.08);
    }

    .google-g-letter {
      width: 20px;
      height: 20px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 1.1rem;
      font-weight: 600;
      color: #4285f4;
      font-family: inherit;
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

    .forgot-link {
      display: block;
      text-align: center;
      padding-top: 12px;
      color: #64748b;
      text-decoration: none;
      font-size: 0.875rem;
      transition: color 0.25s ease;
    }

    .forgot-link:hover {
      color: #1e3a8a;
    }

    .register-link {
      display: block;
      text-align: center;
      padding: 16px 0 0;
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
export class LoginComponent implements OnInit, AfterViewInit {
  @ViewChild('googleButtonRef') googleButtonRef!: ElementRef<HTMLDivElement>;
  form: FormGroup;
  loading = false;
  /** Valor fijo para evitar que el chunk lazy reciba un environment sin googleClientId; el botón real siempre se muestra. */
  readonly googleClientId = DEFAULT_GOOGLE_CLIENT_ID;

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

  ngAfterViewInit(): void {
    if (this.googleClientId && this.googleButtonRef?.nativeElement) {
      setTimeout(() => this.initGoogleButton(), 100);
    } else {
      if (!this.googleClientId) {
        console.warn('[Login] googleClientId vacío: no se cargará el botón de Google. Usa http://localhost:4200 y configura environment.googleClientId.');
      }
    }
  }

  private initGoogleButton(): void {
    const scriptId = 'google-gsi';
    if (document.getElementById(scriptId)) {
      this.renderGoogleButton();
      return;
    }
    const script = document.createElement('script');
    script.id = scriptId;
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => setTimeout(() => this.renderGoogleButton(), 50);
    script.onerror = () => console.error('[Login] Error al cargar el script de Google Identity.');
    document.head.appendChild(script);
  }

  private renderGoogleButton(): void {
    const win = window as Window & { google?: { accounts: { id: { initialize: (c: unknown) => void; renderButton: (el: HTMLElement, o: unknown) => void } } } };
    if (!win.google?.accounts?.id || !this.googleButtonRef?.nativeElement) {
      console.warn('[Login] Google GSI no disponible o contenedor no listo.');
      return;
    }
    win.google.accounts.id.initialize({
      client_id: this.googleClientId,
      callback: (response: { credential: string }) => this.onGoogleCredential(response.credential),
    });
    win.google.accounts.id.renderButton(this.googleButtonRef.nativeElement, {
      type: 'standard',
      theme: 'outline',
      size: 'large',
      text: 'continue_with',
      width: 320,
    });
  }

  onGoogleFallback(): void {
    console.warn('[Login] Botón de respaldo pulsado: googleClientId no está configurado en esta sesión.');
    this.snackBar.open('Configura googleClientId en environment para habilitar inicio con Google.', 'Cerrar', { duration: 4000 });
  }

  onGoogleCredential(idToken: string): void {
    console.log('[Login] Credencial de Google recibida, llamando a la API...');
    this.loading = true;
    this.auth.loginWithGoogle(idToken).subscribe({
      next: () => {
        console.log('[Login] Inicio con Google correcto.');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        console.error('[Login] Error al iniciar con Google:', err?.status, err?.error ?? err?.message, err);
        const msg = err?.error?.message ?? err?.error?.detail ?? err?.message ?? 'Error al iniciar sesión con Google';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
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
