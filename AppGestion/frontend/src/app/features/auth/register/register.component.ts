import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';
import { RegisterRequest } from '../../../core/auth/models/auth.model';

@Component({
    selector: 'app-register',
    imports: [
        ReactiveFormsModule,
        RouterLink,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        TranslateModule,
    ],
    template: `
    <div class="register-wrapper">
      <div class="register-container">
        <div class="register-card">
          <div class="register-logo">
            <img src="assets/noemi-logo.png" alt="Noemí" class="register-logo-img" />
          </div>
          <h1 class="register-title">{{ 'auth.register.title' | translate }}</h1>
          @if (referralToken && referralCheckState === 'checking') {
            <p class="register-ref-status">{{ 'auth.register.referralChecking' | translate }}</p>
          }
          @if (referralToken && referralCheckState === 'bad') {
            <p class="register-ref-warn">{{ 'auth.register.referralInvalid' | translate }}</p>
          }
          @if (referralToken && referralCheckState === 'ok') {
            <p class="register-ref-ok">{{ 'auth.register.referralValid' | translate }}</p>
          }
          <p class="register-subtitle">
            {{ (referralToken ? 'auth.register.subtitleReferral' : 'auth.register.subtitle') | translate }}
          </p>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" class="register-form">
            <mat-form-field appearance="outline" floatLabel="always" class="full-width">
              <mat-label>{{ 'auth.register.nameLabel' | translate }}</mat-label>
              <input matInput formControlName="nombre" [placeholder]="'auth.register.namePlaceholder' | translate" />
              @if (form.get('nombre')?.hasError('required') && form.get('nombre')?.touched) {
                <mat-error>{{ 'auth.register.nameRequired' | translate }}</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline" floatLabel="always" class="full-width">
              <mat-label>{{ 'auth.login.email' | translate }}</mat-label>
              <input matInput formControlName="email" type="email" autocomplete="email" />
              @if (form.get('email')?.hasError('required') && form.get('email')?.touched) {
                <mat-error>{{ 'auth.login.emailRequired' | translate }}</mat-error>
              }
              @if (form.get('email')?.hasError('email')) {
                <mat-error>{{ 'auth.login.emailInvalid' | translate }}</mat-error>
              }
            </mat-form-field>
            <mat-form-field appearance="outline" floatLabel="always" class="full-width">
              <mat-label>{{ 'auth.login.password' | translate }}</mat-label>
              <input matInput formControlName="password" type="password" autocomplete="new-password" />
              @if (form.get('password')?.hasError('required') && form.get('password')?.touched) {
                <mat-error>{{ 'auth.login.passwordRequired' | translate }}</mat-error>
              }
              @if (form.get('password')?.hasError('minlength')) {
                <mat-error>{{ 'auth.register.passwordMinLength' | translate }}</mat-error>
              }
            </mat-form-field>
            <button
              mat-raised-button
              color="primary"
              type="submit"
              [disabled]="loading || (referralToken && referralCheckState === 'checking')"
              class="submit-btn"
            >
              @if (loading) {
                <mat-spinner diameter="24"></mat-spinner>
              } @else {
                {{ 'auth.register.submit' | translate }}
              }
            </button>
          </form>
          <a [routerLink]="['/login']" [queryParams]="loginQueryParams" class="login-link">{{
            'auth.register.loginLink' | translate
          }}</a>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .register-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: linear-gradient(160deg, #e8ecf4 0%, #d4dceb 40%, #c9d1e0 100%);
      transition: background 0.4s ease;
    }

    .register-container {
      width: 100%;
      max-width: 420px;
      animation: registerFadeIn 0.5s ease-out;
    }

    @keyframes registerFadeIn {
      from { opacity: 0; transform: translateY(16px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .register-card {
      background: rgba(255, 255, 255, 0.95);
      border-radius: 28px;
      padding: 40px 36px;
      box-shadow: 0 20px 60px rgba(30, 58, 138, 0.08), 0 8px 24px rgba(0, 0, 0, 0.04);
      transition: box-shadow 0.3s ease, transform 0.3s ease;
    }

    .register-card:hover {
      box-shadow: 0 24px 72px rgba(30, 58, 138, 0.1), 0 12px 32px rgba(0, 0, 0, 0.06);
    }

    .register-logo {
      text-align: center;
      margin-bottom: 24px;
    }

    .register-logo-img {
      height: 96px;
      width: auto;
      object-fit: contain;
    }

    .register-title {
      margin: 0 0 4px 0;
      font-size: 1.6rem;
      font-weight: 600;
      color: #1e293b;
      text-align: center;
      letter-spacing: -0.02em;
    }

    .register-subtitle {
      margin: 0 0 20px 0;
      font-size: 0.95rem;
      color: #64748b;
      text-align: center;
      line-height: 1.45;
    }

    .register-ref-status,
    .register-ref-ok,
    .register-ref-warn {
      margin: 0 0 10px;
      padding: 10px 12px;
      font-size: 0.82rem;
      line-height: 1.35;
      text-align: center;
      border-radius: 12px;
    }

    .register-ref-status {
      color: #475569;
      background: rgba(71, 85, 105, 0.08);
    }

    .register-ref-ok {
      color: #14532d;
      background: #dcfce7;
    }

    .register-ref-warn {
      color: #92400e;
      background: #fef3c7;
    }

    .register-form {
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

    .login-link {
      display: block;
      text-align: center;
      padding: 20px 0 0;
      color: #64748b;
      text-decoration: none;
      font-size: 0.9rem;
      transition: color 0.25s ease;
    }

    .login-link:hover {
      color: #1e3a8a;
    }

    mat-spinner {
      display: inline-block;
      vertical-align: middle;
      margin-right: 8px;
    }
  `]
})
export class RegisterComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);
  form: FormGroup;
  loading = false;

  referralToken: string | null = null;
  referralCheckState: 'idle' | 'checking' | 'ok' | 'bad' = 'idle';
  loginQueryParams: Record<string, string> = {};

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
    this.translate.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.cdr.detectChanges();
    });
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((qp) => {
      const ref = qp.get('ref')?.trim() || null;
      this.referralToken = ref;
      this.loginQueryParams = ref ? { ref } : {};
      if (ref) {
        this.referralCheckState = 'checking';
        this.auth.verifyInviteToken(ref).subscribe({
          next: (r) => {
            this.referralCheckState = r.valid ? 'ok' : 'bad';
          },
          error: () => {
            this.referralCheckState = 'bad';
          },
        });
      } else {
        this.referralCheckState = 'idle';
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    const v = this.form.value as { nombre: string; email: string; password: string };
    const payload: RegisterRequest = {
      nombre: v.nombre,
      email: v.email,
      password: v.password,
    };
    if (this.referralToken && this.referralCheckState === 'ok') {
      payload.referralToken = this.referralToken;
    }
    this.auth.register(payload).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        const msgRaw = err.error?.message ?? err.error ?? err.message ?? '';
        const fb = this.translate.instant('auth.register.registerError');
        const msg =
          typeof msgRaw === 'string' && String(msgRaw).trim() !== '' ? String(msgRaw).trim() : fb;
        console.error('Error en registro:', err);
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 4000 });
      },
    });
  }
}
