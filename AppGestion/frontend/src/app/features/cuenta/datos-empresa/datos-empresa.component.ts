import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { nifValidator } from '../../../shared/validators/nif.validator';
import { RUBRO_AUTONOMO_CATEGORIAS } from './rubro-autonomo.catalog';
import { ConfigService } from '../../../core/services/config.service';
import { Empresa } from '../../../core/models/empresa.model';
import { dataUrlFromStoredBase64 } from '../../../core/utils/image-data-url';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

/** Límite servidor ~400 KB; margen en cliente. */
const MAX_IMAGE_BYTES = 380_000;

/** Email opcional: el vacío es válido y los espacios sobrantes no bloquean el guardado. */
function emailOpcional(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = control.value;
    if (typeof raw !== 'string' || raw.trim() === '') {
      return null;
    }
    const trimmed = raw.trim();
    if (trimmed === raw) {
      return Validators.email(control);
    }
    return Validators.email({ value: trimmed } as AbstractControl);
  };
}

@Component({
    selector: 'app-datos-empresa',
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
        MatSelectModule,
        TranslateModule,
    ],
    templateUrl: './datos-empresa.component.html',
    styleUrl: './datos-empresa.component.scss'
})
export class DatosEmpresaComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly config = inject(ConfigService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly rubroCategorias = RUBRO_AUTONOMO_CATEGORIAS;

  private readonly etiquetasCampo: Record<string, string> = {
    nombre: 'acctCo.nameLbl',
    direccion: 'acctCo.addrLbl',
    codigoPostal: 'acctCo.postLbl',
    provincia: 'acctCo.provLbl',
    pais: 'acctCo.countryLbl',
    nif: 'acctCo.nifLbl',
    telefono: 'acctCo.phoneLbl',
    email: 'acctCo.emailLbl',
    notasPiePresupuesto: 'acctCo.footerBudget',
    notasPieFactura: 'acctCo.footerInvoice',
    rubroAutonomoCodigo: 'acctCo.rubroLbl',
  };

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(200)]],
    direccion: ['', [Validators.required, Validators.maxLength(255)]],
    codigoPostal: ['', [Validators.required, Validators.maxLength(10)]],
    provincia: ['', [Validators.required, Validators.maxLength(100)]],
    pais: ['España', [Validators.required, Validators.maxLength(100)]],
    nif: ['', [Validators.required, nifValidator()]],
    telefono: ['', [Validators.maxLength(50)]],
    email: ['', [Validators.maxLength(150), emailOpcional()]],
    notasPiePresupuesto: ['', [Validators.maxLength(1000)]],
    notasPieFactura: ['', [Validators.maxLength(1000)]],
    rubroAutonomoCodigo: [''],
  });

  logoPreviewSrc: string | null = null;
  logoCambiada: 'none' | 'set' | 'clear' = 'none';
  nuevaLogoBase64: string | null = null;
  tieneLogoGuardada = false;

  /** Nombres de los campos que impiden guardar, para mostrarlos junto al botón. */
  camposInvalidos(): string {
    return Object.entries(this.form.controls)
      .filter(([, control]) => control.invalid)
      .map(([name]) => this.translate.instant(this.etiquetasCampo[name] ?? name))
      .join(', ');
  }

  ngOnInit(): void {
    const oauth = this.route.snapshot.queryParamMap.get('oauth');
    if (oauth === 'success') {
      this.snackBar.open(this.translate.instant('snack.companyEmailOk'), this.translate.instant('common.close'), {
        duration: 4000,
      });
      void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    } else if (oauth === 'error') {
      this.snackBar.open(this.translate.instant('snack.companyEmailFail'), this.translate.instant('common.close'), {
        duration: 5000,
      });
      void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    }
    this.cargar();
  }

  cargar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.config.getEmpresa().subscribe({
      next: (e: Empresa) => this.aplicarEmpresa(e),
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
        this.snackBar.open(this.translate.instant('snack.companyDataLoadFail'), this.translate.instant('common.close'), {
          duration: 4000,
        });
      },
    });
  }

  private aplicarEmpresa(e: Empresa): void {
    this.tieneLogoGuardada = !!(e.tieneLogo && e.logoImagenBase64);
    this.logoPreviewSrc = dataUrlFromStoredBase64(e.logoImagenBase64 ?? null);
    this.logoCambiada = 'none';
    this.nuevaLogoBase64 = null;

    this.form.patchValue({
      nombre: (e.nombre ?? '').trim(),
      direccion: (e.direccion ?? '').trim(),
      codigoPostal: (e.codigoPostal ?? '').trim(),
      provincia: (e.provincia ?? '').trim(),
      pais: (e.pais ?? 'España').trim() || 'España',
      nif: (e.nif ?? '').trim(),
      telefono: (e.telefono ?? '').trim(),
      email: (e.email ?? '').trim(),
      notasPiePresupuesto: e.notasPiePresupuesto ?? '',
      notasPieFactura: e.notasPieFactura ?? '',
      rubroAutonomoCodigo: e.rubroAutonomoCodigo ?? '',
    });
    this.form.markAsPristine();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
    }
    this.loading.set(false);
  }

  onLogoFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!/^image\/(png|jpeg|jpg|gif)$/i.test(file.type)) {
      this.snackBar.open(this.translate.instant('snack.companyImageFormat'), this.translate.instant('common.close'), {
        duration: 4000,
      });
      input.value = '';
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      this.snackBar.open(
        this.translate.instant('snack.companyImageTooBig', { maxKb: Math.round(MAX_IMAGE_BYTES / 1024) }),
        this.translate.instant('common.close'),
        { duration: 5000 },
      );
      input.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const r = reader.result as string;
      const comma = r.indexOf(',');
      const b64 = comma >= 0 ? r.slice(comma + 1) : r;
      this.logoPreviewSrc = r;
      this.nuevaLogoBase64 = b64;
      this.logoCambiada = 'set';
      this.tieneLogoGuardada = true;
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  quitarLogo(): void {
    this.logoPreviewSrc = null;
    this.nuevaLogoBase64 = null;
    this.logoCambiada = 'clear';
    this.tieneLogoGuardada = false;
    this.form.markAsDirty();
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open(
        this.translate.instant('acctCo.saveBlocked', { fields: this.camposInvalidos() }),
        this.translate.instant('common.close'),
        { duration: 5000 },
      );
      document
        .querySelector('.empresa-page input.ng-invalid, .empresa-page textarea.ng-invalid, .empresa-page mat-select.ng-invalid')
        ?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
    const v = this.form.getRawValue();
    const payload: Record<string, unknown> = {
      nombre: v.nombre.trim(),
      direccion: v.direccion.trim(),
      codigoPostal: v.codigoPostal.trim(),
      provincia: v.provincia.trim(),
      pais: v.pais.trim(),
      nif: v.nif.trim(),
      telefono: v.telefono.trim() || undefined,
      email: v.email.trim() || undefined,
      notasPiePresupuesto: v.notasPiePresupuesto.trim() || undefined,
      notasPieFactura: v.notasPieFactura.trim() || undefined,
      rubroAutonomoCodigo: v.rubroAutonomoCodigo?.trim() ?? '',
    };

    if (this.logoCambiada === 'clear') {
      payload['logoImagenBase64'] = '';
    } else if (this.logoCambiada === 'set' && this.nuevaLogoBase64) {
      payload['logoImagenBase64'] = this.nuevaLogoBase64;
    }

    this.saving.set(true);
    this.config.saveEmpresa(payload as Partial<Empresa>).subscribe({
      next: (updated) => {
        this.aplicarEmpresa(updated);
        this.snackBar.open(this.translate.instant('snack.companyDataSaved'), this.translate.instant('common.close'), {
          duration: 3000,
        });
        this.saving.set(false);
      },
      error: (err) => {
        this.saving.set(false);
        const raw = err.error?.message || err.error?.detail || err.error?.error;
        const msg =
          typeof raw === 'string' && raw.trim() !== ''
            ? raw.trim()
            : this.translate.instant('snack.companySaveFail');
        this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
      },
    });
  }

}
