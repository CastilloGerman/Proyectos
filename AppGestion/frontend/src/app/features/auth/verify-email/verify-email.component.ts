import { ChangeDetectorRef, Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../../core/auth/auth.service';

type VerifyState = 'verifying' | 'ok' | 'invalid';

/** Destino del enlace de confirmación de email enviado al registrarse (`/verify-email?token=...`). */
@Component({
  selector: 'app-verify-email',
  imports: [RouterLink, MatButtonModule, MatProgressSpinnerModule, TranslateModule],
  template: `
    <div class="verify-wrapper">
      <div class="verify-card">
        <img src="assets/noemi-logo.png" alt="Noemí" class="verify-logo" />
        @switch (state) {
          @case ('verifying') {
            <mat-spinner diameter="32"></mat-spinner>
            <p class="verify-text">{{ 'auth.register.verifyEmailVerifying' | translate }}</p>
          }
          @case ('ok') {
            <p class="verify-text verify-ok">{{ 'auth.register.verifyEmailSuccess' | translate }}</p>
            <a mat-raised-button color="primary" routerLink="/login" class="verify-btn">{{
              'auth.register.verifyEmailGoLogin' | translate
            }}</a>
          }
          @case ('invalid') {
            <p class="verify-text verify-error">{{ 'auth.register.verifyEmailInvalid' | translate }}</p>
            <a mat-raised-button color="primary" routerLink="/login" class="verify-btn">{{
              'auth.register.verifyEmailGoLogin' | translate
            }}</a>
          }
        }
      </div>
    </div>
  `,
  styles: [
    `
      .verify-wrapper {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 24px;
        background: linear-gradient(160deg, #e8ecf4 0%, #d4dceb 40%, #c9d1e0 100%);
      }
      .verify-card {
        width: 100%;
        max-width: 420px;
        background: rgba(255, 255, 255, 0.95);
        border-radius: 28px;
        padding: 40px 36px;
        text-align: center;
        box-shadow: 0 20px 60px rgba(30, 58, 138, 0.08), 0 8px 24px rgba(0, 0, 0, 0.04);
      }
      .verify-logo {
        height: 72px;
        width: auto;
        margin-bottom: 24px;
      }
      .verify-text {
        margin: 16px 0 24px;
        font-size: 1rem;
        line-height: 1.5;
        color: #334155;
      }
      .verify-ok {
        color: #14532d;
      }
      .verify-error {
        color: #b91c1c;
      }
      .verify-btn {
        width: 100%;
        padding: 12px 24px;
        border-radius: 14px;
      }
    `,
  ],
})
export class VerifyEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly translate = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  state: VerifyState = 'verifying';

  ngOnInit(): void {
    this.translate.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.cdr.detectChanges());
    const token = this.route.snapshot.queryParamMap.get('token')?.trim();
    if (!token) {
      this.state = 'invalid';
      return;
    }
    this.auth.verifyEmail(token).subscribe({
      next: () => {
        this.state = 'ok';
        this.cdr.detectChanges();
      },
      error: () => {
        this.state = 'invalid';
        this.cdr.detectChanges();
      },
    });
  }
}
