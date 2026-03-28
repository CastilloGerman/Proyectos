import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { take } from 'rxjs';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { nifValidator } from '../../../shared/validators/nif.validator';
import { RUBRO_AUTONOMO_CATEGORIAS } from './rubro-autonomo.catalog';
import { ConfigService } from '../../../core/services/config.service';
import { Empresa } from '../../../core/models/empresa.model';
import { dataUrlFromStoredBase64 } from '../../../core/utils/image-data-url';

/** Límite servidor ~400 KB; margen en cliente. */
const MAX_IMAGE_BYTES = 380_000;

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
        MatDividerModule,
        MatExpansionModule,
        MatSelectModule,
        MatRadioModule,
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

  readonly rubroCategorias = RUBRO_AUTONOMO_CATEGORIAS;

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
    email: ['', [Validators.maxLength(150), Validators.email]],
    notasPiePresupuesto: ['', [Validators.maxLength(1000)]],
    notasPieFactura: ['', [Validators.maxLength(1000)]],
    mailHost: ['', [Validators.maxLength(100)]],
    mailPort: [587 as number],
    mailUsername: ['', [Validators.maxLength(150)]],
    mailPassword: ['', [Validators.maxLength(255)]],
    rubroAutonomoCodigo: [''],
    emailProvider: ['system' as 'system' | 'gmail' | 'outlook' | 'smtp_legacy'],
    oauthOnFailure: ['system' as 'system' | 'fail'],
    systemFromOverride: ['', [Validators.maxLength(255)]],
  });

  mailConfigurado = false;
  oauthConnected = false;
  /** Si el servidor tiene credenciales OAuth (evita 503 al pulsar Conectar). */
  googleOAuthConfigured = false;
  microsoftOAuthConfigured = false;

  logoPreviewSrc: string | null = null;
  logoCambiada: 'none' | 'set' | 'clear' = 'none';
  nuevaLogoBase64: string | null = null;
  tieneLogoGuardada = false;

  oauthConnecting = false;

  ngOnInit(): void {
    const oauth = this.route.snapshot.queryParamMap.get('oauth');
    if (oauth === 'success') {
      this.snackBar.open('Cuenta de correo conectada correctamente', 'Cerrar', { duration: 4000 });
      void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    } else if (oauth === 'error') {
      this.snackBar.open('No se pudo completar la conexión con el proveedor de correo', 'Cerrar', { duration: 5000 });
      void this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    }
    this.cargar();
  }

  cargar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.config.getEmpresa().subscribe({
      next: (e: Empresa) => {
        this.aplicarEmpresa(e);
        this.config.getEmailOAuthStatus().subscribe({
          next: (s) => {
            this.googleOAuthConfigured = s.googleOAuthConfigured;
            this.microsoftOAuthConfigured = s.microsoftOAuthConfigured;
            this.oauthConnected = s.oauthConnected;
            this.loading.set(false);
          },
          error: () => {
            this.googleOAuthConfigured = false;
            this.microsoftOAuthConfigured = false;
            this.loading.set(false);
          },
        });
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
        this.snackBar.open('No se pudieron cargar los datos de empresa', 'Cerrar', { duration: 4000 });
      },
    });
  }

  private aplicarEmpresa(e: Empresa): void {
    this.mailConfigurado = e.mailConfigurado ?? false;
    this.oauthConnected = e.oauthConnected ?? false;
    this.tieneLogoGuardada = !!(e.tieneLogo && e.logoImagenBase64);
    this.logoPreviewSrc = dataUrlFromStoredBase64(e.logoImagenBase64 ?? null);
    this.logoCambiada = 'none';
    this.nuevaLogoBase64 = null;

    this.form.patchValue({
      nombre: e.nombre ?? '',
      direccion: e.direccion ?? '',
      codigoPostal: e.codigoPostal ?? '',
      provincia: e.provincia ?? '',
      pais: e.pais ?? 'España',
      nif: e.nif ?? '',
      telefono: e.telefono ?? '',
      email: e.email ?? '',
      notasPiePresupuesto: e.notasPiePresupuesto ?? '',
      notasPieFactura: e.notasPieFactura ?? '',
      mailHost: e.mailHost ?? '',
      mailPort: e.mailPort ?? 587,
      mailUsername: e.mailUsername ?? '',
      mailPassword: '',
      rubroAutonomoCodigo: e.rubroAutonomoCodigo ?? '',
      emailProvider: (e.emailProvider as 'system' | 'gmail' | 'outlook' | 'smtp_legacy') ?? 'system',
      oauthOnFailure: (e.oauthOnFailure as 'system' | 'fail') ?? 'system',
      systemFromOverride: e.systemFromOverride ?? '',
    });
    this.form.markAsPristine();
    this.loading.set(false);
  }

  onLogoFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!/^image\/(png|jpeg|jpg|webp)$/i.test(file.type)) {
      this.snackBar.open('Formato no admitido. Usa PNG, JPEG o WebP.', 'Cerrar', { duration: 4000 });
      input.value = '';
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      this.snackBar.open(`La imagen supera ${Math.round(MAX_IMAGE_BYTES / 1024)} KB. Reduce tamaño o comprime.`, 'Cerrar', {
        duration: 5000,
      });
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
      mailHost: v.mailHost.trim() || undefined,
      mailPort: v.mailPort,
      mailUsername: v.mailUsername.trim() || undefined,
      rubroAutonomoCodigo: v.rubroAutonomoCodigo?.trim() ?? '',
    };
    if (!v.mailPassword?.trim()) {
      delete payload['mailPassword'];
    } else {
      payload['mailPassword'] = v.mailPassword;
    }

    if (this.logoCambiada === 'clear') {
      payload['logoImagenBase64'] = '';
    } else if (this.logoCambiada === 'set' && this.nuevaLogoBase64) {
      payload['logoImagenBase64'] = this.nuevaLogoBase64;
    }

    payload['emailProvider'] = v.emailProvider;
    payload['oauthOnFailure'] = v.oauthOnFailure;
    const sfo = v.systemFromOverride?.trim();
    payload['systemFromOverride'] = sfo || '';

    this.saving.set(true);
    this.config.saveEmpresa(payload as Partial<Empresa>).subscribe({
      next: (updated) => {
        this.aplicarEmpresa(updated);
        this.snackBar.open('Datos de empresa guardados', 'Cerrar', { duration: 3000 });
        this.saving.set(false);
      },
      error: (err) => {
        this.saving.set(false);
        const msg =
          err.error?.message || err.error?.detail || err.error?.error || 'No se pudo guardar. Revisa los datos.';
        this.snackBar.open(typeof msg === 'string' ? msg : 'No se pudo guardar', 'Cerrar', { duration: 5000 });
      },
    });
  }

  conectarGoogle(): void {
    this.oauthConnecting = true;
    this.config
      .getGoogleEmailAuthorizeUrl()
      .pipe(take(1))
      .subscribe({
        next: (r) => {
          window.location.href = r.url;
        },
        error: (err) => {
          this.oauthConnecting = false;
          const msg = this.httpErrorMessage(err, 'No se pudo iniciar la conexión con Google');
          this.snackBar.open(msg, 'Cerrar', { duration: 8000 });
        },
      });
  }

  conectarMicrosoft(): void {
    this.oauthConnecting = true;
    this.config
      .getMicrosoftEmailAuthorizeUrl()
      .pipe(take(1))
      .subscribe({
        next: (r) => {
          window.location.href = r.url;
        },
        error: (err) => {
          this.oauthConnecting = false;
          const msg = this.httpErrorMessage(err, 'No se pudo iniciar la conexión con Microsoft');
          this.snackBar.open(msg, 'Cerrar', { duration: 8000 });
        },
      });
  }

  private httpErrorMessage(err: unknown, fallback: string): string {
    const e = err as { error?: { detail?: string; message?: string; error?: string } };
    const raw = e?.error?.detail ?? e?.error?.message ?? e?.error?.error ?? fallback;
    return typeof raw === 'string' ? raw : fallback;
  }

  desconectarOAuth(): void {
    this.config.disconnectEmailOAuth().subscribe({
      next: () => {
        this.snackBar.open('Cuenta de correo desconectada', 'Cerrar', { duration: 3000 });
        this.cargar();
      },
      error: () => this.snackBar.open('No se pudo desconectar', 'Cerrar', { duration: 4000 }),
    });
  }
}
