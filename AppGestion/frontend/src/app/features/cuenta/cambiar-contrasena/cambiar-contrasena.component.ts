import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
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
import { Subscription } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { passwordMatchValidator } from '../../../shared/validators/password-match.validator';

@Component({
  selector: 'app-cambiar-contrasena',
  standalone: true,
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
  templateUrl: './cambiar-contrasena.component.html',
  styleUrl: './cambiar-contrasena.component.scss',
})
export class CambiarContrasenaComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly saving = signal(false);
  hideCurrent = true;
  hideNew = true;
  hideConfirm = true;

  private sub?: Subscription;

  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required, Validators.maxLength(128)]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(128)]],
    confirmPassword: ['', [Validators.required, passwordMatchValidator('newPassword')]],
  });

  ngOnInit(): void {
    this.sub = this.form.controls.newPassword.valueChanges.subscribe(() => {
      this.form.controls.confirmPassword.updateValueAndValidity({ emitEvent: false });
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.saving.set(true);
    this.auth.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.form.reset();
        this.form.markAsPristine();
        this.snackBar.open('Contraseña actualizada correctamente', 'Cerrar', { duration: 4000 });
        this.saving.set(false);
      },
      error: (err) => {
        this.saving.set(false);
        const msg =
          err.error?.message ||
          err.error?.detail ||
          err.error?.error ||
          'No se pudo cambiar la contraseña';
        this.snackBar.open(typeof msg === 'string' ? msg : 'Error al guardar', 'Cerrar', { duration: 5000 });
      },
    });
  }
}
