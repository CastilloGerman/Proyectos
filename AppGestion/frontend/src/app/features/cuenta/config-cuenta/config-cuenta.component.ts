import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService, UsuarioResponse } from '../../../core/auth/auth.service';

@Component({
    selector: 'app-config-cuenta',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatSlideToggleModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatDividerModule,
    ],
    templateUrl: './config-cuenta.component.html',
    styleUrl: './config-cuenta.component.scss'
})
export class ConfigCuentaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);
  readonly me = signal<UsuarioResponse | null>(null);

  readonly form = this.fb.nonNullable.group({
    emailNotifyBilling: true,
    emailNotifyDocuments: true,
    emailNotifyMarketing: false,
  });

  ngOnInit(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.auth.refreshUser().subscribe({
      next: (data) => {
        if (data) {
          this.me.set(data);
          this.form.patchValue({
            emailNotifyBilling: data.emailNotifyBilling ?? true,
            emailNotifyDocuments: data.emailNotifyDocuments ?? true,
            emailNotifyMarketing: data.emailNotifyMarketing ?? false,
          });
          this.form.markAsPristine();
        } else {
          this.loadError.set(true);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  guardar(): void {
    if (this.form.invalid || this.form.pristine) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.auth
      .updateAccountSettings({
        emailNotifyBilling: v.emailNotifyBilling,
        emailNotifyDocuments: v.emailNotifyDocuments,
        emailNotifyMarketing: v.emailNotifyMarketing,
      })
      .subscribe({
        next: (updated) => {
          this.me.set(updated);
          this.form.patchValue(
            {
              emailNotifyBilling: updated.emailNotifyBilling ?? true,
              emailNotifyDocuments: updated.emailNotifyDocuments ?? true,
              emailNotifyMarketing: updated.emailNotifyMarketing ?? false,
            },
            { emitEvent: false }
          );
          this.form.markAsPristine();
          this.snackBar.open('Preferencias guardadas', 'Cerrar', { duration: 3000 });
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          const msg = err.error?.message || err.error?.detail || 'No se pudo guardar';
          this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
        },
      });
  }

  reintentar(): void {
    this.ngOnInit();
  }
}
