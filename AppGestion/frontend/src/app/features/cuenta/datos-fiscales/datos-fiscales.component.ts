import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { ConfigService } from '../../../core/services/config.service';
import { Empresa } from '../../../core/models/empresa.model';
import { nifIvaIntraValidator } from '../../../shared/validators/nif-iva-intra.validator';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

/** Value persisted in API (Spanish labels); translated label via `labelKey`. */
type FiscalRegimenOption = { readonly value: string; readonly labelKey: string };

const REGIMEN_DEFS: readonly FiscalRegimenOption[] = [
  { value: 'Régimen general del IVA', labelKey: 'acctFiscal.reg.GENERAL' },
  { value: 'Recargo de equivalencia', labelKey: 'acctFiscal.reg.REC_EQUIV' },
  { value: 'Exento (operaciones exentas sin derecho a deducción)', labelKey: 'acctFiscal.reg.EXENTO' },
  { value: 'Inversión del sujeto pasivo', labelKey: 'acctFiscal.reg.INV_SUJ_PASS' },
  { value: 'Operaciones no sujetas a IVA por reglas de localización', labelKey: 'acctFiscal.reg.NO_SUJETO' },
  { value: 'Operaciones sujetas al IGIC (Canarias)', labelKey: 'acctFiscal.reg.IGIC' },
  { value: 'Operaciones sujetas al IPSI (Ceuta y Melilla)', labelKey: 'acctFiscal.reg.IPSI' },
  {
    value: 'Autónomo en módulos (estimación objetiva) — consultar asesoría',
    labelKey: 'acctFiscal.reg.MODULES',
  },
  { value: 'Otro (especificar en descripción de actividad)', labelKey: 'acctFiscal.reg.OTHER_SPEC' },
];

const KNOWN_REGIMEN_VALUES = new Set(REGIMEN_DEFS.map((r) => r.value));
const REGIMEN_DEFAULT_VALUE = REGIMEN_DEFS[0].value;

@Component({
    selector: 'app-datos-fiscales',
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
        TranslateModule,
    ],
    templateUrl: './datos-fiscales.component.html',
    styleUrl: './datos-fiscales.component.scss'
})
export class DatosFiscalesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly config = inject(ConfigService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);
  readonly empresaResumen = signal<Empresa | null>(null);
  readonly regimenChoices = signal<readonly FiscalRegimenOption[]>([...REGIMEN_DEFS]);

  readonly form = this.fb.nonNullable.group({
    regimenIvaPrincipal: [REGIMEN_DEFAULT_VALUE, [Validators.required, Validators.maxLength(120)]],
    descripcionActividad: ['', [Validators.maxLength(500)]],
    nifIntracomunitario: ['', [Validators.maxLength(20), nifIvaIntraValidator()]],
    epigrafeIae: ['', [Validators.maxLength(30)]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.config.getEmpresa().subscribe({
      next: (e) => {
        this.empresaResumen.set(e);
        const regimen = e.regimenIvaPrincipal?.trim() || REGIMEN_DEFAULT_VALUE;
        if (!KNOWN_REGIMEN_VALUES.has(regimen)) {
          this.regimenChoices.set([{ value: regimen, labelKey: '' }, ...REGIMEN_DEFS]);
        } else {
          this.regimenChoices.set([...REGIMEN_DEFS]);
        }
        this.form.patchValue({
          regimenIvaPrincipal: regimen,
          descripcionActividad: e.descripcionActividadFiscal || '',
          nifIntracomunitario: e.nifIntracomunitario || '',
          epigrafeIae: e.epigrafeIae || '',
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

  domicilioUnaLinea(e: Empresa): string {
    const parts = [e.direccion, e.codigoPostal, e.provincia, e.pais].filter((x) => x && String(x).trim());
    return parts.length ? parts.join(', ') : '';
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.config
      .patchDatosFiscales({
        regimenIvaPrincipal: v.regimenIvaPrincipal.trim(),
        descripcionActividad: v.descripcionActividad.trim(),
        nifIntracomunitario: v.nifIntracomunitario.replace(/\s/g, '').trim(),
        epigrafeIae: v.epigrafeIae.trim(),
      })
      .subscribe({
        next: (updated) => {
          this.empresaResumen.set(updated);
          const reg = updated.regimenIvaPrincipal?.trim() || REGIMEN_DEFAULT_VALUE;
          if (!KNOWN_REGIMEN_VALUES.has(reg)) {
            this.regimenChoices.set([{ value: reg, labelKey: '' }, ...REGIMEN_DEFS]);
          } else {
            this.regimenChoices.set([...REGIMEN_DEFS]);
          }
          this.form.patchValue({
            regimenIvaPrincipal: reg,
            descripcionActividad: updated.descripcionActividadFiscal || '',
            nifIntracomunitario: updated.nifIntracomunitario || '',
            epigrafeIae: updated.epigrafeIae || '',
          });
          this.form.markAsPristine();
          this.snackBar.open(this.translate.instant('snack.taxDataSaved'), this.translate.instant('common.close'), {
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
              : this.translate.instant('snack.errorSave');
          this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
        },
      });
  }
}
