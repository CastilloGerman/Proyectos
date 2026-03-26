import { Component, DestroyRef, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, firstValueFrom, startWith } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DomSanitizer, SafeResourceUrl, SafeUrl } from '@angular/platform-browser';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConfigService, PlantillasPdfPreviewPayload } from '../../../core/services/config.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Empresa } from '../../../core/models/empresa.model';
import {
  PLACEHOLDER_CATALOG,
  PREVIEW_MOCK_BY_SCENARIO,
  PREVIEW_SCENARIO_LABELS,
  SAMPLE_LONG_FOOTER_TEXT,
  DocumentPreviewMock,
  PlaceholderDef,
  PlantillaPdfPreviewEscenario,
} from './document-template.models';

const MAX_PIE = 1000;

const PH_PRESU =
  'Ejemplo: Presupuesto válido 30 días. El IVA se detalla en el cuerpo del documento.';
const PH_FACT =
  'Ejemplo: Gracias por su confianza. Para cualquier consulta puede llamarnos o escribirnos.';

@Component({
    selector: 'app-plantillas-documentos',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatButtonToggleModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatTooltipModule,
    ],
    templateUrl: './plantillas-documentos.component.html',
    styleUrl: './plantillas-documentos.component.scss'
})
export class PlantillasDocumentosComponent implements OnInit, OnDestroy {
  readonly placeholderPresupuesto = PH_PRESU;
  readonly placeholderFactura = PH_FACT;

  readonly escenarioOptions: { value: PlantillaPdfPreviewEscenario; label: string }[] = (
    Object.keys(PREVIEW_SCENARIO_LABELS) as PlantillaPdfPreviewEscenario[]
  ).map((value) => ({ value, label: PREVIEW_SCENARIO_LABELS[value] }));

  private readonly fb = inject(FormBuilder);
  private readonly config = inject(ConfigService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);
  readonly maxPie = MAX_PIE;

  /** 0 = presupuesto, 1 = factura */
  readonly previewTabIndex = signal(0);

  /** Vista rápida (HTML) vs PDF generado por la aplicación. */
  readonly previewMode = signal<'html' | 'pdf'>('html');

  /** Perfil de líneas de ejemplo (HTML + mismo PDF al pedir vista exacta). */
  readonly dataProfile = signal<PlantillaPdfPreviewEscenario>('DEFAULT');

  readonly activeMock = computed(() => PREVIEW_MOCK_BY_SCENARIO[this.dataProfile()]);

  readonly pdfPreviewLoading = signal(false);
  readonly pdfPreviewError = signal<string | null>(null);

  pdfSafeUrl: SafeResourceUrl | null = null;
  private pdfObjectUrl: string | null = null;
  private pdfRequestSeq = 0;

  readonly form = this.fb.nonNullable.group({
    notasPiePresupuesto: ['', [Validators.maxLength(MAX_PIE)]],
    notasPieFactura: ['', [Validators.maxLength(MAX_PIE)]],
  });

  private readonly formValue = toSignal(this.form.valueChanges.pipe(startWith(this.form.getRawValue())), {
    initialValue: { notasPiePresupuesto: '', notasPieFactura: '' },
  });

  readonly placeholders = PLACEHOLDER_CATALOG;

  /** Empresa cargada (misma respuesta que GET /config/empresa) para cabecera de la vista rápida. */
  readonly empresaPreview = signal<Empresa | null>(null);
  /** Logo en data URL seguro para <img [src]>. */
  readonly logoPreviewSafeUrl = signal<SafeUrl | null>(null);

  readonly empresaPreviewNombre = computed(() => {
    const n = this.empresaPreview()?.nombre?.trim();
    return n || 'Tu negocio';
  });

  readonly empresaPreviewSubline = computed(() => {
    const e = this.empresaPreview();
    if (!e) {
      return 'Tus datos (los pones en «Nombre, dirección y logo»)';
    }
    const parts: string[] = [];
    if (e.direccion?.trim()) {
      parts.push(e.direccion.trim());
    }
    const cpProv = [e.codigoPostal?.trim(), e.provincia?.trim()].filter(Boolean).join(' ');
    if (cpProv) {
      parts.push(cpProv);
    }
    if (e.pais?.trim() && e.pais.trim() !== 'España') {
      parts.push(e.pais.trim());
    }
    if (e.nif?.trim()) {
      parts.push(`NIF ${e.nif.trim()}`);
    }
    return parts.length > 0 ? parts.join(' · ') : 'Tus datos (los pones en «Nombre, dirección y logo»)';
  });

  readonly textoPieVistaPrevia = computed(() => {
    const idx = this.previewTabIndex();
    const v = this.formValue();
    const mock = this.activeMock();
    const raw = idx === 0 ? v.notasPiePresupuesto : v.notasPieFactura;
    return this.sustituirPlaceholders(raw ?? '', idx === 0 ? 'PRESUPUESTO' : 'FACTURA', mock);
  });

  ngOnInit(): void {
    this.form.valueChanges
      .pipe(debounceTime(320), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.loading() && this.previewMode() === 'pdf') {
          void this.refreshPdfPreview();
        }
      });
    this.cargar();
  }

  ngOnDestroy(): void {
    this.revokePdfUrl();
  }

  get puedeEditar(): boolean {
    return this.auth.canMutate();
  }

  setPreviewMode(mode: 'html' | 'pdf'): void {
    this.previewMode.set(mode);
    if (mode === 'html') {
      this.revokePdfUrl();
      this.pdfPreviewError.set(null);
      this.pdfPreviewLoading.set(false);
    } else {
      void this.refreshPdfPreview();
    }
  }

  onPreviewModeToggle(value: string | null | undefined): void {
    if (value === 'html' || value === 'pdf') {
      this.setPreviewMode(value);
    }
  }

  retryPdfPreview(): void {
    void this.refreshPdfPreview();
  }

  /** Valor del grupo de botones «Presupuesto / Factura» (Material suele usar string). */
  previewDocToggleValue(): string {
    return String(this.previewTabIndex());
  }

  onPickPreviewDocument(value: string | null | undefined): void {
    const n = value === '1' ? 1 : 0;
    this.previewTabIndex.set(n);
    const id = n === 0 ? 'plant-texto-presupuesto' : 'plant-texto-factura';
    queueMicrotask(() => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'center' }));
    if (this.previewMode() === 'pdf') {
      void this.refreshPdfPreview();
    }
  }

  onEditBlockFocus(which: 0 | 1): void {
    this.previewTabIndex.set(which);
    if (this.previewMode() === 'pdf') {
      void this.refreshPdfPreview();
    }
  }

  onEscenarioChange(value: PlantillaPdfPreviewEscenario): void {
    this.dataProfile.set(value);
    if (this.previewMode() === 'pdf') {
      void this.refreshPdfPreview();
    }
  }

  tooltipPlaceholder(p: PlaceholderDef): string {
    return `Ejemplo en el documento: ${p.ejemplo}`;
  }

  copyPlaceholder(p: PlaceholderDef): void {
    void navigator.clipboard.writeText(p.token).then(
      () => {
        this.snackBar.open(`Copiado. Pégalo en el cuadro de arriba (Ctrl+V).`, 'Cerrar', {
          duration: 4000,
        });
      },
      () => {
        this.snackBar.open('No se pudo copiar automáticamente. Escríbelo a mano si hace falta.', 'Cerrar', {
          duration: 3500,
        });
      },
    );
  }

  pegarPieLargoEjemplo(): void {
    const key = this.previewTabIndex() === 0 ? 'notasPiePresupuesto' : 'notasPieFactura';
    this.form.patchValue({ [key]: SAMPLE_LONG_FOOTER_TEXT });
    this.form.markAsDirty();
  }

  fmtEuro(value: number): string {
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(value);
  }

  private sustituirPlaceholders(raw: string, tipo: 'PRESUPUESTO' | 'FACTURA', mock: DocumentPreviewMock): string {
    const docNum = tipo === 'PRESUPUESTO' ? 'PRES-2025-0099' : mock.numeroDocumento;
    const fecha = new Date(mock.fechaEmision + 'T12:00:00').toLocaleDateString('es-ES');
    const mapa: Record<string, string> = {
      '{{client_name}}': mock.clienteNombre,
      '{{client_tax_id}}': mock.clienteNif,
      '{{client_address}}': mock.clienteDireccion,
      '{{doc_number}}': docNum,
      '{{doc_date}}': fecha,
      '{{subtotal}}': this.fmtEuro(mock.subtotal),
      '{{tax_total}}': this.fmtEuro(mock.iva),
      '{{total}}': this.fmtEuro(mock.total),
    };
    let out = raw ?? '';
    for (const [k, v] of Object.entries(mapa)) {
      out = out.split(k).join(v);
    }
    return out;
  }

  cargar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.config.getEmpresa().subscribe({
      next: (e: Empresa) => {
        this.form.patchValue({
          notasPiePresupuesto: e.notasPiePresupuesto ?? '',
          notasPieFactura: e.notasPieFactura ?? '',
        });
        this.form.markAsPristine();
        this.applyEmpresaPreview(e);
        this.loading.set(false);
        if (this.previewMode() === 'pdf') {
          void this.refreshPdfPreview();
        }
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
        this.snackBar.open('No se pudieron cargar los textos.', 'Cerrar', { duration: 4000 });
      },
    });
  }

  guardar(): void {
    if (!this.puedeEditar || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.config
      .patchPlantillasPdf({
        notasPiePresupuesto: v.notasPiePresupuesto.trim() || null,
        notasPieFactura: v.notasPieFactura.trim() || null,
      })
      .subscribe({
        next: () => {
          this.form.markAsPristine();
          this.snackBar.open('Guardado. Los próximos PDF usarán estos textos.', 'Cerrar', {
            duration: 4500,
          });
          this.saving.set(false);
          if (this.previewMode() === 'pdf') {
            void this.refreshPdfPreview();
          }
        },
        error: (err) => {
          this.saving.set(false);
          const msg =
            err?.error?.message ?? err?.error?.detail ?? 'No se pudo guardar. Revisa la conexión o tus permisos.';
          this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
        },
      });
  }

  private applyEmpresaPreview(e: Empresa): void {
    this.empresaPreview.set(e);
    const b64 = e.logoImagenBase64?.trim();
    if (!b64) {
      this.logoPreviewSafeUrl.set(null);
      return;
    }
    this.logoPreviewSafeUrl.set(this.sanitizer.bypassSecurityTrustUrl(buildImageDataUrlFromBase64(b64)));
  }

  private revokePdfUrl(): void {
    if (this.pdfObjectUrl) {
      URL.revokeObjectURL(this.pdfObjectUrl);
      this.pdfObjectUrl = null;
    }
    this.pdfSafeUrl = null;
  }

  private async refreshPdfPreview(): Promise<void> {
    if (this.previewMode() !== 'pdf' || this.loading()) {
      return;
    }
    const seq = ++this.pdfRequestSeq;
    this.pdfPreviewLoading.set(true);
    this.pdfPreviewError.set(null);
    const v = this.form.getRawValue();
    const idx = this.previewTabIndex();
    const body: PlantillasPdfPreviewPayload = {
      tipo: idx === 0 ? 'PRESUPUESTO' : 'FACTURA',
      notasPie: idx === 0 ? v.notasPiePresupuesto : v.notasPieFactura,
      escenario: this.dataProfile(),
    };
    try {
      const buffer = await firstValueFrom(this.config.postPlantillasPdfPreview(body));
      if (seq !== this.pdfRequestSeq) {
        return;
      }
      this.revokePdfUrl();
      const blob = new Blob([buffer], { type: 'application/pdf' });
      this.pdfObjectUrl = URL.createObjectURL(blob);
      this.pdfSafeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.pdfObjectUrl);
    } catch (e) {
      if (seq !== this.pdfRequestSeq) {
        return;
      }
      this.revokePdfUrl();
      let msg = 'No se pudo mostrar el PDF de ejemplo.';
      if (e instanceof HttpErrorResponse && e.error instanceof ArrayBuffer) {
        try {
          const t = new TextDecoder().decode(e.error);
          const j = JSON.parse(t) as { message?: string; error?: string };
          if (typeof j.message === 'string') {
            msg = j.message;
          } else if (typeof j.error === 'string') {
            msg = j.error;
          }
        } catch {
          /* ignore */
        }
      }
      this.pdfPreviewError.set(msg);
    } finally {
      if (seq === this.pdfRequestSeq) {
        this.pdfPreviewLoading.set(false);
      }
    }
  }
}

/**
 * Construye una data URL con MIME acorde a la firma del binario (PNG/JPEG/GIF/WebP).
 * El API solo envía el Base64 crudo.
 */
function buildImageDataUrlFromBase64(b64: string): string {
  const clean = b64.replace(/\s/g, '');
  try {
    const headLen = Math.min(clean.length, 32);
    const head = clean.substring(0, headLen);
    const padded = head + '='.repeat((4 - (head.length % 4)) % 4);
    const binaryString = atob(padded);
    const n = Math.min(binaryString.length, 12);
    const bytes = new Uint8Array(n);
    for (let i = 0; i < n; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    if (bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
      return `data:image/jpeg;base64,${clean}`;
    }
    if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) {
      return `data:image/png;base64,${clean}`;
    }
    if (bytes[0] === 0x47 && bytes[1] === 0x49 && bytes[2] === 0x46) {
      return `data:image/gif;base64,${clean}`;
    }
    if (
      bytes[0] === 0x52 &&
      bytes[1] === 0x49 &&
      bytes[2] === 0x46 &&
      bytes[8] === 0x57 &&
      bytes[9] === 0x45 &&
      bytes[10] === 0x42 &&
      bytes[11] === 0x50
    ) {
      return `data:image/webp;base64,${clean}`;
    }
  } catch {
    /* ignore */
  }
  return `data:image/png;base64,${clean}`;
}
