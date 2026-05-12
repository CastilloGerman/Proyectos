import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { switchMap } from 'rxjs';
import { AuthService, UsuarioResponse } from '../../../core/auth/auth.service';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-totp-2fa',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatDividerModule,
    ],
    templateUrl: './totp-2fa.component.html',
    styleUrl: './totp-2fa.component.scss'
})
export class Totp2FaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  readonly me = signal<UsuarioResponse | null>(null);
  readonly loadingMe = signal(true);
  readonly startingSetup = signal(false);
  readonly confirming = signal(false);
  readonly disabling = signal(false);
  readonly cancelling = signal(false);

  /** Datos locales del último POST start (QR caduca en servidor por tiempo). */
  readonly setupOtpUrl = signal<string | null>(null);
  readonly setupSecret = signal<string | null>(null);
  readonly setupExpiresMin = signal<number>(10);
  readonly qrDataUrl = signal<string | null>(null);

  hideDisablePassword = true;

  readonly confirmForm = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  readonly disableForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required, Validators.maxLength(128)]],
    totpCode: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  ngOnInit(): void {
    this.auth.refreshUser().subscribe({
      next: (u) => {
        this.me.set(u);
        this.loadingMe.set(false);
      },
      error: () => {
        this.loadingMe.set(false);
      },
    });
  }

  startSetup(): void {
    this.startingSetup.set(true);
    this.auth.startTotpSetup().subscribe({
      next: (res) => {
        this.setupOtpUrl.set(res.otpAuthUrl);
        this.setupSecret.set(res.secretBase32);
        this.setupExpiresMin.set(res.pendingExpiresInMinutes ?? 10);
        this.startingSetup.set(false);
        this.buildQr(res.otpAuthUrl);
        this.auth.refreshUser().subscribe((u) => this.me.set(u));
      },
      error: (err) => {
        this.startingSetup.set(false);
        const raw = err.error?.message || err.error?.detail;
        const msg =
          typeof raw === 'string' && raw.trim() !== ''
            ? raw.trim()
            : this.translate.instant('snack.totpEnableFail');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }

  private async buildQr(url: string): Promise<void> {
    try {
      const { default: QRCode } = await import('qrcode');
      const dataUrl = await QRCode.toDataURL(url, { width: 220, margin: 2, errorCorrectionLevel: 'M' });
      this.qrDataUrl.set(dataUrl);
    } catch {
      this.qrDataUrl.set(null);
    }
  }

  copySecret(): void {
    const s = this.setupSecret();
    if (!s) return;
    navigator.clipboard.writeText(s).then(
      () =>
        this.snackBar.open(this.translate.instant('snack.totpSecretCopied'), this.translate.instant('common.close'), {
          duration: 2500,
        }),
      () =>
        this.snackBar.open(this.translate.instant('snack.totpCopyFail'), this.translate.instant('common.close'), {
          duration: 3000,
        }),
    );
  }

  confirmSetup(): void {
    if (this.confirmForm.invalid) {
      this.confirmForm.markAllAsTouched();
      return;
    }
    const code = this.confirmForm.getRawValue().code.trim();
    this.confirming.set(true);
    this.auth.confirmTotpSetup(code).subscribe({
      next: (u) => {
        this.me.set(u);
        this.setupOtpUrl.set(null);
        this.setupSecret.set(null);
        this.qrDataUrl.set(null);
        this.confirmForm.reset();
        this.confirming.set(false);
        this.snackBar.open(this.translate.instant('snack.totpEnabled'), this.translate.instant('common.close'), {
          duration: 4000,
        });
      },
      error: (err) => {
        this.confirming.set(false);
        const raw = err.error?.message || err.error?.detail;
        const msg =
          typeof raw === 'string' && raw.trim() !== ''
            ? raw.trim()
            : this.translate.instant('snack.totpEnableFail');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }

  cancelSetup(): void {
    this.cancelling.set(true);
    this.auth.cancelTotpSetup().subscribe({
      next: () => {
        this.setupOtpUrl.set(null);
        this.setupSecret.set(null);
        this.qrDataUrl.set(null);
        this.confirmForm.reset();
        this.cancelling.set(false);
        this.auth.refreshUser().subscribe((u) => this.me.set(u));
        this.snackBar.open(this.translate.instant('snack.totpCancelled'), this.translate.instant('common.close'), {
          duration: 3000,
        });
      },
      error: () => {
        this.cancelling.set(false);
        this.snackBar.open(this.translate.instant('snack.totpCancelFail'), this.translate.instant('common.close'), {
          duration: 4000,
        });
      },
    });
  }

  disableTotp(): void {
    if (this.disableForm.invalid) {
      this.disableForm.markAllAsTouched();
      return;
    }
    const { currentPassword, totpCode } = this.disableForm.getRawValue();
    this.disabling.set(true);
    this.auth
      .disableTotp({ currentPassword, totpCode: totpCode.trim() })
      .pipe(switchMap(() => this.auth.refreshUser()))
      .subscribe({
        next: (u) => {
          this.me.set(u);
          this.disableForm.reset();
          this.disabling.set(false);
          this.snackBar.open(this.translate.instant('snack.totpDisabled'), this.translate.instant('common.close'), {
            duration: 4000,
          });
        },
        error: (err) => {
          this.disabling.set(false);
          const raw = err.error?.message || err.error?.detail || err.error?.error;
          const msg =
            typeof raw === 'string' && String(raw).trim() !== ''
              ? String(raw).trim()
              : this.translate.instant('snack.totpDisableFail');
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
        },
      });
  }
}
