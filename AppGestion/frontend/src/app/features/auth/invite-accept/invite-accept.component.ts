import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
    selector: 'app-invite-accept',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatSnackBarModule,
    ],
    template: `
    <div class="invite-wrap">
      <mat-card class="invite-card">
        <mat-card-header>
          <mat-card-title>Crear cuenta con enlace de referido</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (loading) {
            <p>Comprobando invitación…</p>
          } @else if (!valid) {
            <p class="error">Este enlace no es válido o ha caducado.</p>
            <a mat-button routerLink="/login">Ir al inicio de sesión</a>
          } @else {
            <p class="email-line">Email: <strong>{{ emailInvitado }}</strong></p>
            <p class="info-line">
              Tendrás periodo de prueba con permisos completos. Cuando termine, necesitarás una suscripción
              activa para seguir creando o editando; sin ella solo podrás consultar tus datos.
            </p>
            <form [formGroup]="form" (ngSubmit)="onSubmit()">
              <mat-form-field appearance="outline" class="full">
                <mat-label>Nombre</mat-label>
                <input matInput formControlName="nombre" autocomplete="name" />
              </mat-form-field>
              <mat-form-field appearance="outline" class="full">
                <mat-label>Contraseña</mat-label>
                <input matInput type="password" formControlName="password" autocomplete="new-password" />
              </mat-form-field>
              <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || saving">
                Crear cuenta
              </button>
            </form>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
    styles: [`
    .invite-wrap {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      background: linear-gradient(160deg, var(--app-bg-warm, #2c1810) 0%, var(--app-bg-deep, #0f172a) 55%, #0c1222 100%);
    }
    .invite-card { max-width: 420px; width: 100%; }
    .full { width: 100%; display: block; margin-bottom: 12px; }
    .error { color: #b91c1c; }
    .email-line { margin-bottom: 8px; font-size: 14px; }
    .info-line { font-size: 13px; color: rgba(0,0,0,0.65); line-height: 1.45; margin-bottom: 16px; }
  `]
})
export class InviteAcceptComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  token = '';
  loading = true;
  valid = false;
  emailInvitado = '';
  saving = false;
  form = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private auth: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((pm) => {
      this.token = pm.get('token') ?? '';
      if (!this.token) {
        this.loading = false;
        this.valid = false;
        return;
      }
      this.auth.verifyInviteToken(this.token).subscribe({
        next: (r) => {
          this.loading = false;
          this.valid = !!r.valid;
          this.emailInvitado = r.email ?? '';
        },
        error: () => {
          this.loading = false;
          this.valid = false;
        },
      });
    });
  }

  onSubmit(): void {
    if (this.form.invalid || !this.token) return;
    this.saving = true;
    const v = this.form.value;
    this.auth
      .acceptInvite({
        token: this.token,
        nombre: v.nombre!.trim(),
        password: v.password!,
      })
      .subscribe({
        next: () => {
          this.snackBar.open('Cuenta creada', 'Cerrar', { duration: 3000 });
          this.router.navigate(['/dashboard']);
          this.saving = false;
        },
        error: (err) => {
          this.snackBar.open(err.error?.message || 'No se pudo crear la cuenta', 'Cerrar', { duration: 5000 });
          this.saving = false;
        },
      });
  }
}
