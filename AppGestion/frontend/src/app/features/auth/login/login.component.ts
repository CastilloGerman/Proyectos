import { AfterViewInit, Component, OnInit, ViewChild, ElementRef, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AboutComponent } from '../about/about.component';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
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
    imports: [
        ReactiveFormsModule,
        FormsModule,
        RouterLink,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        AboutComponent,
    ],
    template: `
    <div class="login-page" id="login-page-top">
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
            <mat-form-field appearance="outline" floatLabel="always" class="full-width">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" type="email" autocomplete="email">
              <mat-hint class="login-email-hint">ej. nombre&#64;correo.com</mat-hint>
              @if (form.get('email')?.hasError('required') && form.get('email')?.touched) {
                <mat-error>El email es obligatorio</mat-error>
              }
              @if (form.get('email')?.hasError('email')) {
                <mat-error>Email inválido</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline" floatLabel="always" class="full-width">
              <mat-label>Contraseña</mat-label>
              <input matInput formControlName="password" type="password" autocomplete="current-password">
              @if (form.get('password')?.hasError('required') && form.get('password')?.touched) {
                <mat-error>La contraseña es obligatoria</mat-error>
              }
            </mat-form-field>
            @if (totpStep()) {
              <mat-form-field appearance="outline" floatLabel="always" class="full-width">
                <mat-label>Código de verificación (2FA)</mat-label>
                <input
                  matInput
                  formControlName="totpCode"
                  inputmode="numeric"
                  maxlength="6"
                  autocomplete="one-time-code"
                />
                <mat-hint>6 dígitos</mat-hint>
                @if (form.get('totpCode')?.hasError('required') && form.get('totpCode')?.touched) {
                  <mat-error>Introduce el código de 6 dígitos de tu app</mat-error>
                }
                @if (form.get('totpCode')?.hasError('pattern') && form.get('totpCode')?.touched) {
                  <mat-error>Debe tener 6 dígitos</mat-error>
                }
              </mat-form-field>
              <p class="login-totp-hint">Abre tu app de autenticación y copia el código actual.</p>
            }
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
          @if (googleTotpStep()) {
            <div class="google-totp-box">
              <p class="login-totp-hint">Tu cuenta tiene 2FA activo. Introduce el código de tu app.</p>
              <mat-form-field appearance="outline" floatLabel="always" class="full-width">
                <mat-label>Código 2FA</mat-label>
                <input
                  matInput
                  [(ngModel)]="googleTotpCode"
                  [ngModelOptions]="{ standalone: true }"
                  inputmode="numeric"
                  maxlength="6"
                  autocomplete="one-time-code"
                />
                <mat-hint>6 dígitos</mat-hint>
              </mat-form-field>
              <button
                mat-stroked-button
                type="button"
                class="google-totp-submit"
                [disabled]="loading || !isSixDigits(googleTotpCode)"
                (click)="submitGoogleTotp()"
              >
                Confirmar e iniciar sesión
              </button>
            </div>
          }
          <a routerLink="/forgot-password" class="forgot-link">¿Olvidaste tu contraseña?</a>
          <a routerLink="/register" class="register-link">¿No tienes cuenta? Regístrate</a>
        </div>
      </div>
      <button
        type="button"
        class="scroll-down-btn"
        aria-label="Desplazar a la sección sobre Noemí"
        (click)="scrollToAbout()"
      >
        <span class="scroll-down-btn-label">Nuestra historia</span>
        <svg class="scroll-down-btn-icon" width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <path fill="currentColor" d="M12 15.5l-6-6 1.4-1.4L12 12.7l4.6-4.6L18 9.5l-6 6z"/>
        </svg>
      </button>
    </div>
    <div #aboutAnchor class="login-about-anchor">
      <app-about />
    </div>
    </div>
  `,
    styles: [`
    .login-page {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }

    .login-wrapper {
      min-height: 100vh;
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px;
      padding-bottom: 80px;
      background: linear-gradient(160deg, #e8ecf4 0%, #d4dceb 40%, #c9d1e0 100%);
      transition: background 0.4s ease;
    }

    .login-about-anchor {
      flex-shrink: 0;
    }

    @keyframes scrollHint {
      0%, 100% { transform: translateX(-50%) translateY(0); }
      50% { transform: translateX(-50%) translateY(5px); }
    }

    .scroll-down-btn {
      position: absolute;
      bottom: 28px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 2;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      padding: 8px 16px;
      border: none;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.85);
      color: #1e3a8a;
      font-family: inherit;
      font-size: 0.8rem;
      font-weight: 600;
      cursor: pointer;
      box-shadow: 0 4px 16px rgba(30, 58, 138, 0.12);
      animation: scrollHint 2.5s ease-in-out infinite;
      transition: background 0.2s ease, color 0.2s ease;
    }

    .scroll-down-btn:hover {
      background: #fff;
      color: #172554;
      animation-play-state: paused;
      transform: translateX(-50%);
    }

    .scroll-down-btn-label {
      letter-spacing: 0.02em;
    }

    .scroll-down-btn-icon {
      display: block;
      opacity: 0.9;
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

    .login-email-hint {
      font-size: 0.8rem;
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

    .login-totp-hint {
      margin: -4px 0 12px;
      font-size: 0.85rem;
      color: #64748b;
      line-height: 1.4;
    }

    .google-totp-box {
      margin-top: 16px;
      padding: 16px;
      border-radius: 12px;
      background: rgba(30, 64, 175, 0.06);
      border: 1px solid rgba(30, 64, 175, 0.12);
    }

    .google-totp-submit {
      width: 100%;
      margin-top: 8px;
    }

    /* Modo oscuro: misma paleta que el resto de la app (evita texto claro sobre tarjeta blanca). */
    :host-context(html.app-dark-theme) .login-wrapper {
      background: linear-gradient(165deg, #0c1220 0%, #141b2d 45%, #0f1419 100%);
    }

    :host-context(html.app-dark-theme) .login-card {
      background: var(--app-bg-card);
      border: 1px solid var(--app-border);
      box-shadow: var(--app-shadow-lg);
      color: var(--app-text-primary);
    }

    :host-context(html.app-dark-theme) .login-card input::placeholder {
      color: var(--app-text-muted);
      opacity: 1;
    }

    :host-context(html.app-dark-theme) .login-card .mat-mdc-form-field {
      --mdc-outlined-text-field-input-text-color: var(--app-text-primary);
      --mdc-outlined-text-field-label-text-color: var(--app-text-secondary);
      --mdc-outlined-text-field-hover-label-text-color: var(--app-text-primary);
      --mdc-outlined-text-field-focus-label-text-color: var(--app-text-secondary);
      --mdc-outlined-text-field-outline-color: var(--app-border);
      --mdc-outlined-text-field-hover-outline-color: rgba(148, 163, 184, 0.45);
      --mdc-outlined-text-field-focus-outline-color: #818cf8;
      --mat-form-field-outlined-label-text-color: var(--app-text-secondary);
      --mat-form-field-outlined-label-text-populated-color: var(--app-text-secondary);
      --mat-form-field-error-text-color: #fca5a5;
      --mat-form-field-hint-text-color: var(--app-text-muted);
    }

    :host-context(html.app-dark-theme) .login-title {
      color: var(--app-text-primary);
    }

    :host-context(html.app-dark-theme) .login-tagline {
      color: #a5b4fc;
    }

    :host-context(html.app-dark-theme) .login-subtitle {
      color: var(--app-text-secondary);
    }

    :host-context(html.app-dark-theme) .login-divider span {
      color: var(--app-text-muted);
    }

    :host-context(html.app-dark-theme) .forgot-link,
    :host-context(html.app-dark-theme) .register-link {
      color: var(--app-text-secondary);
    }

    :host-context(html.app-dark-theme) .forgot-link:hover,
    :host-context(html.app-dark-theme) .register-link:hover {
      color: #c7d2fe;
    }

    :host-context(html.app-dark-theme) .login-totp-hint {
      color: var(--app-text-secondary);
    }

    :host-context(html.app-dark-theme) .google-totp-box {
      background: rgba(99, 102, 241, 0.12);
      border-color: rgba(129, 140, 248, 0.35);
    }

    :host-context(html.app-dark-theme) .google-fallback-btn {
      color: var(--app-text-primary);
      background: rgba(255, 255, 255, 0.06);
      border-color: var(--app-border);
    }

    :host-context(html.app-dark-theme) .google-fallback-btn:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    :host-context(html.app-dark-theme) .scroll-down-btn {
      background: rgba(26, 34, 44, 0.92);
      color: #c7d2fe;
      border: 1px solid var(--app-border);
      box-shadow: var(--app-shadow-md);
    }

    :host-context(html.app-dark-theme) .scroll-down-btn:hover {
      background: rgba(36, 46, 60, 0.98);
      color: #e0e7ff;
    }
  `]
})
export class LoginComponent implements OnInit, AfterViewInit {
  @ViewChild('googleButtonRef') googleButtonRef!: ElementRef<HTMLDivElement>;
  @ViewChild('aboutAnchor') aboutAnchor!: ElementRef<HTMLElement>;
  form: FormGroup;
  loading = false;
  readonly totpStep = signal(false);
  readonly googleTotpStep = signal(false);
  googleTotpCode = '';
  private pendingGoogleToken: string | null = null;
  /** Evita [GSI_LOGGER]: initialize() múltiples veces (p. ej. doble montaje en dev). */
  private static gsiInitializedForClientId: string | null = null;
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
      totpCode: [''],
    });
  }

  isSixDigits(v: string): boolean {
    return /^\d{6}$/.test((v || '').trim());
  }

  private isTotpRequiredError(err: unknown): boolean {
    const e = err as { error?: { message?: string; detail?: string } };
    const msg = e?.error?.message ?? e?.error?.detail;
    return msg === 'TOTP_REQUERIDO';
  }

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  scrollToAbout(): void {
    this.aboutAnchor?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
    if (LoginComponent.gsiInitializedForClientId !== this.googleClientId) {
      win.google.accounts.id.initialize({
        client_id: this.googleClientId,
        callback: (response: { credential: string }) => this.onGoogleCredential(response.credential),
      });
      LoginComponent.gsiInitializedForClientId = this.googleClientId;
    }
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
    this.pendingGoogleToken = idToken;
    this.googleTotpStep.set(false);
    this.googleTotpCode = '';
    this.callGoogleLogin(idToken);
  }

  private callGoogleLogin(idToken: string, totpCode?: string): void {
    this.loading = true;
    this.auth.loginWithGoogle(idToken, totpCode).subscribe({
      next: () => {
        console.log('[Login] Inicio con Google correcto.');
        this.loading = false;
        this.googleTotpStep.set(false);
        this.pendingGoogleToken = null;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        console.error('[Login] Error al iniciar con Google:', err?.status, err?.error ?? err?.message, err);
        if (this.isTotpRequiredError(err)) {
          this.googleTotpStep.set(true);
          return;
        }
        const msg = err?.error?.message ?? err?.error?.detail ?? err?.message ?? 'Error al iniciar sesión con Google';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }

  submitGoogleTotp(): void {
    const token = this.pendingGoogleToken;
    if (!token || !this.isSixDigits(this.googleTotpCode)) return;
    this.callGoogleLogin(token, this.googleTotpCode.trim());
  }

  onSubmit(): void {
    const step = this.totpStep();
    if (!step) {
      const emailCtrl = this.form.get('email');
      const passCtrl = this.form.get('password');
      if (emailCtrl?.invalid || passCtrl?.invalid) {
        this.form.markAllAsTouched();
        return;
      }
    } else {
      this.form.get('totpCode')?.setValidators([Validators.required, Validators.pattern(/^\d{6}$/)]);
      this.form.get('totpCode')?.updateValueAndValidity({ emitEvent: false });
      if (this.form.get('totpCode')?.invalid) {
        this.form.get('totpCode')?.markAsTouched();
        return;
      }
    }

    this.loading = true;
    const raw = this.form.getRawValue();
    this.auth
      .login({
        email: raw.email,
        password: raw.password,
        totpCode: step ? raw.totpCode.trim() : undefined,
      })
      .subscribe({
        next: () => {
          this.loading = false;
          this.totpStep.set(false);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.loading = false;
          if (!step && this.isTotpRequiredError(err)) {
            this.totpStep.set(true);
            this.form.get('totpCode')?.setValidators([Validators.required, Validators.pattern(/^\d{6}$/)]);
            this.form.get('totpCode')?.updateValueAndValidity({ emitEvent: false });
            this.snackBar.open('Introduce el código de tu app de autenticación', 'Cerrar', { duration: 4500 });
            return;
          }
          this.snackBar.open(err.error?.message || err.error?.detail || 'Credenciales inválidas', 'Cerrar', {
            duration: 4000,
          });
        },
      });
  }
}
