import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../../core/auth/auth.service';
import { ConfigService } from '../../../core/services/config.service';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { formatIbanDisplay, ibanValidator } from '../../../shared/validators/iban.validator';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';

const BIZUM_MOVIL_ES = /^[6-9]\d{8}$/;

function normalizarDigitosBizum(raw: string): string {
  let d = raw.replace(/\D/g, '');
  if (d.length > 9 && d.startsWith('34')) {
    d = d.slice(2);
  }
  return d;
}

function bizumOpcionalValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = control.value;
    if (raw == null || String(raw).trim() === '') return null;
    const d = normalizarDigitosBizum(String(raw));
    if (!BIZUM_MOVIL_ES.test(d)) return { bizumInvalido: true };
    return null;
  };
}

@Component({
  selector: 'app-metodos-pago',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
  ],
  templateUrl: './metodos-pago.component.html',
  styleUrl: './metodos-pago.component.scss',
})
export class MetodosPagoComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly config = inject(ConfigService);
  private readonly auth = inject(AuthService);
  private readonly subscriptionApi = inject(SubscriptionService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);
  readonly openingPortal = signal(false);

  readonly portalDisponible = computed(() => this.auth.user()?.billingPortalAvailable === true);

  readonly form = this.fb.nonNullable.group({
    defaultMetodoPago: ['Transferencia', [Validators.required]],
    defaultCondicionesPago: ['', [Validators.maxLength(200)]],
    ibanCuenta: ['', [ibanValidator()]],
    bizumTelefono: ['', [Validators.maxLength(20), bizumOpcionalValidator()]],
  });

  readonly vistaPdf = computed(() => {
    const v = this.form.getRawValue();
    const metodo = v.defaultMetodoPago || 'Transferencia';
    const ibanFmt = formatIbanDisplay(v.ibanCuenta?.replace(/\s/g, '') || '');
    const parts: string[] = [metodo];
    if (metodo === 'Transferencia' && ibanFmt) parts.push(`IBAN ${ibanFmt}`);
    if (metodo === 'Bizum' && v.bizumTelefono?.trim()) parts.push(`Bizum ${v.bizumTelefono.trim()}`);
    return parts.join(' · ');
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.config.getEmpresa().subscribe({
      next: (e) => {
        this.form.patchValue({
          defaultMetodoPago: e.defaultMetodoPago || 'Transferencia',
          defaultCondicionesPago: e.defaultCondicionesPago || '',
          ibanCuenta: e.ibanCuenta ? formatIbanDisplay(e.ibanCuenta) : '',
          bizumTelefono: e.bizumTelefono || '',
        });
        this.form.markAsPristine();
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    const biz = v.bizumTelefono.trim();
    this.config
      .patchMetodosCobro({
        defaultMetodoPago: v.defaultMetodoPago.trim(),
        defaultCondicionesPago: v.defaultCondicionesPago.trim(),
        ibanCuenta: v.ibanCuenta.replace(/\s/g, '').trim(),
        bizumTelefono: biz ? normalizarDigitosBizum(biz) : '',
      })
      .subscribe({
        next: (e) => {
          this.form.patchValue({
            defaultMetodoPago: e.defaultMetodoPago || 'Transferencia',
            defaultCondicionesPago: e.defaultCondicionesPago || '',
            ibanCuenta: e.ibanCuenta ? formatIbanDisplay(e.ibanCuenta) : '',
            bizumTelefono: e.bizumTelefono || '',
          });
          this.form.markAsPristine();
          this.snackBar.open('Métodos de cobro guardados', 'Cerrar', { duration: 3000 });
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          const msg = err.error?.message || err.error?.detail || err.error?.error || 'No se pudo guardar';
          this.snackBar.open(typeof msg === 'string' ? msg : 'Error al guardar', 'Cerrar', { duration: 5000 });
        },
      });
  }

  abrirPortalStripe(): void {
    this.openingPortal.set(true);
    this.subscriptionApi.createPortalSession().subscribe({
      next: (res) => {
        if (res.portalUrl) window.location.href = res.portalUrl;
        else this.openingPortal.set(false);
      },
      error: (err) => {
        this.openingPortal.set(false);
        this.snackBar.open(err.error?.error || 'No se pudo abrir el portal', 'Cerrar', { duration: 4000 });
      },
    });
  }

  irSuscripcion(): void {
    void this.router.navigate(['/cuenta/suscripcion']);
  }
}
