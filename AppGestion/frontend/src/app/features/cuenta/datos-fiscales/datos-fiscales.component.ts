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

const REGIMEN_BASE: readonly string[] = [
  'Régimen general del IVA',
  'Recargo de equivalencia',
  'Exento (operaciones exentas sin derecho a deducción)',
  'Inversión del sujeto pasivo',
  'Operaciones no sujetas a IVA por reglas de localización',
  'Operaciones sujetas al IGIC (Canarias)',
  'Operaciones sujetas al IPSI (Ceuta y Melilla)',
  'Autónomo en módulos (estimación objetiva) — consultar asesoría',
  'Otro (especificar en descripción de actividad)',
];

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
    ],
    templateUrl: './datos-fiscales.component.html',
    styleUrl: './datos-fiscales.component.scss'
})
export class DatosFiscalesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly config = inject(ConfigService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);
  readonly empresaResumen = signal<Empresa | null>(null);
  readonly regimenChoices = signal<string[]>([...REGIMEN_BASE]);

  readonly form = this.fb.nonNullable.group({
    regimenIvaPrincipal: ['Régimen general del IVA', [Validators.required, Validators.maxLength(120)]],
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
        const regimen = e.regimenIvaPrincipal?.trim() || 'Régimen general del IVA';
        if (!REGIMEN_BASE.includes(regimen)) {
          this.regimenChoices.set([regimen, ...REGIMEN_BASE]);
        } else {
          this.regimenChoices.set([...REGIMEN_BASE]);
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
    return parts.length ? parts.join(', ') : '—';
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
          this.form.patchValue({
            regimenIvaPrincipal: updated.regimenIvaPrincipal || 'Régimen general del IVA',
            descripcionActividad: updated.descripcionActividadFiscal || '',
            nifIntracomunitario: updated.nifIntracomunitario || '',
            epigrafeIae: updated.epigrafeIae || '',
          });
          this.form.markAsPristine();
          this.snackBar.open('Datos fiscales guardados', 'Cerrar', { duration: 3000 });
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          const msg = err.error?.message || err.error?.detail || err.error?.error || 'No se pudo guardar';
          this.snackBar.open(typeof msg === 'string' ? msg : 'Error al guardar', 'Cerrar', { duration: 5000 });
        },
      });
  }
}
