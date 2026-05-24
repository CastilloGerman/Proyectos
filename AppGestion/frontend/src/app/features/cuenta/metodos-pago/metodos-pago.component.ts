import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ConfigService } from '../../../core/services/config.service';
import { Empresa } from '../../../core/models/empresa.model';
import { formatIbanDisplay, ibanValidator, normalizarIbanParaValidar } from '../../../shared/validators/iban.validator';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { map } from 'rxjs';

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
        MatSlideToggleModule,
        MatCheckboxModule,
        TranslateModule,
    ],
    templateUrl: './metodos-pago.component.html',
    styleUrl: './metodos-pago.component.scss'
})
export class MetodosPagoComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly config = inject(ConfigService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  private readonly localeTick = toSignal(this.translate.onLangChange.pipe(map(() => Date.now())), {
    initialValue: 0,
  });

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly savingRecordatorios = signal(false);
  readonly loadError = signal(false);

  readonly form = this.fb.nonNullable.group({
    defaultMetodoPago: ['Transferencia', [Validators.required]],
    defaultCondicionesPago: ['', [Validators.maxLength(200)]],
    ibanCuenta: ['', [ibanValidator()]],
    nombreBanco: ['', [Validators.maxLength(100)]],
    titularCuenta: ['', [Validators.maxLength(150)]],
    bizumTelefono: ['', [Validators.maxLength(20), bizumOpcionalValidator()]],
  });

  readonly recordatorioForm = this.fb.nonNullable.group({
    activo: [false],
    dia7: [true],
    dia15: [true],
    dia30: [true],
  });

  readonly vistaPdf = computed(() => {
    void this.localeTick();
    const v = this.form.getRawValue();
    const metodoRaw = v.defaultMetodoPago || 'Transferencia';
    const metodo = this.metodoPagoLabel(metodoRaw);
    const ibanFmt = formatIbanDisplay(v.ibanCuenta || '');
    const parts: string[] = [metodo];
    if (metodoRaw === 'Transferencia') {
      if (ibanFmt) {
        parts.push(this.translate.instant('acctPay.previewIbanFmt', { iban: ibanFmt }));
      }
      if (v.titularCuenta?.trim()) {
        parts.push(this.translate.instant('acctPay.previewHolderFmt', { name: v.titularCuenta.trim() }));
      }
      if (v.nombreBanco?.trim()) parts.push(v.nombreBanco.trim());
      const hasBank = !!(ibanFmt || v.titularCuenta?.trim() || v.nombreBanco?.trim());
      if (!hasBank) {
        parts.push(this.translate.instant('acctPay.previewNoBank'));
      }
    }
    if (metodoRaw === 'Bizum' && v.bizumTelefono?.trim()) {
      parts.push(this.translate.instant('acctPay.previewBizumFmt', { phone: v.bizumTelefono.trim() }));
    }
    return parts.filter(Boolean).join(' · ');
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
          nombreBanco: e.nombreBanco || '',
          titularCuenta: e.titularCuenta || '',
          bizumTelefono: e.bizumTelefono || '',
        });
        this.form.markAsPristine();
        this.aplicarRecordatoriosDesdeEmpresa(e);
        this.recordatorioForm.markAsPristine();
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
        ibanCuenta: normalizarIbanParaValidar(v.ibanCuenta),
        nombreBanco: v.nombreBanco.trim(),
        titularCuenta: v.titularCuenta.trim(),
        bizumTelefono: biz ? normalizarDigitosBizum(biz) : '',
      })
      .subscribe({
        next: (e) => {
          this.form.patchValue({
            defaultMetodoPago: e.defaultMetodoPago || 'Transferencia',
            defaultCondicionesPago: e.defaultCondicionesPago || '',
            ibanCuenta: e.ibanCuenta ? formatIbanDisplay(e.ibanCuenta) : '',
            nombreBanco: e.nombreBanco || '',
            titularCuenta: e.titularCuenta || '',
            bizumTelefono: e.bizumTelefono || '',
          });
          this.form.markAsPristine();
          this.snackBar.open(this.translate.instant('snack.payMethodsSaved'), this.translate.instant('common.close'), {
            duration: 3000,
          });
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          const raw = err.error?.message || err.error?.detail || err.error?.error;
          const msg =
            typeof raw === 'string' && String(raw).trim() !== ''
              ? String(raw).trim()
              : this.translate.instant('snack.payMethodsSaveFail');
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
        },
      });
  }

  /** Formato español legible (ESkk BBBB GGGG CC …) al salir del campo; no cambia el valor si está incompleto. */
  onIbanBlur(): void {
    const c = this.form.controls.ibanCuenta;
    const raw = c.value?.trim();
    if (!raw) return;
    const norm = normalizarIbanParaValidar(raw);
    if (norm.length === 0) return;
    if (norm.startsWith('ES') && norm.length !== 24) return;
    const formatted = formatIbanDisplay(raw);
    if (formatted !== c.value) {
      c.setValue(formatted, { emitEvent: true });
    }
  }

  private aplicarRecordatoriosDesdeEmpresa(e: Empresa): void {
    const dias = e.recordatorioClienteDias?.length ? e.recordatorioClienteDias : [7, 15, 30];
    this.recordatorioForm.patchValue({
      activo: e.recordatorioClienteActivo ?? false,
      dia7: dias.includes(7),
      dia15: dias.includes(15),
      dia30: dias.includes(30),
    });
  }

  private diasRecordatorioSeleccionados(): number[] {
    const v = this.recordatorioForm.getRawValue();
    const out: number[] = [];
    if (v.dia7) out.push(7);
    if (v.dia15) out.push(15);
    if (v.dia30) out.push(30);
    return out;
  }

  guardarRecordatorios(): void {
    const activo = this.recordatorioForm.controls.activo.value;
    const dias = this.diasRecordatorioSeleccionados();
    if (activo && dias.length === 0) {
      this.snackBar.open(this.translate.instant('snack.remindersNeedTerm'), this.translate.instant('common.close'), {
        duration: 5000,
      });
      return;
    }
    this.savingRecordatorios.set(true);
    const diasPayload = dias.length > 0 ? dias : [7, 15, 30];
    this.config
      .patchRecordatoriosCobro({
        recordatorioClienteActivo: activo,
        recordatorioClienteDias: diasPayload,
      })
      .subscribe({
        next: (emp) => {
          this.aplicarRecordatoriosDesdeEmpresa(emp);
          this.recordatorioForm.markAsPristine();
          this.snackBar.open(this.translate.instant('snack.remindersSaved'), this.translate.instant('common.close'), {
            duration: 3000,
          });
          this.savingRecordatorios.set(false);
        },
        error: (err) => {
          this.savingRecordatorios.set(false);
          const raw = err.error?.message || err.error?.detail || err.error?.error;
          const msg =
            typeof raw === 'string' && String(raw).trim() !== ''
              ? String(raw).trim()
              : this.translate.instant('snack.remindersSaveFail');
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
        },
      });
  }

  private metodoPagoLabel(api: string): string {
    const key: Record<string, string> = {
      Transferencia: 'acctPay.mTransfer',
      Bizum: 'acctPay.mBizum',
      Tarjeta: 'acctPay.mCard',
      Efectivo: 'acctPay.mCash',
    };
    const k = key[api];
    return k ? this.translate.instant(k) : api;
  }
}
