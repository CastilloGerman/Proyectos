import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { nifValidator } from '../validators/nif.validator';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ConfigService } from '../../core/services/config.service';
import { Empresa } from '../../core/models/empresa.model';
import { dataUrlFromStoredBase64 } from '../../core/utils/image-data-url';

export type ConfigContext = 'presupuesto' | 'factura' | 'mail';

@Component({
  selector: 'app-config-empresa-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
    MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>Configuración de plantillas</h2>
    <mat-dialog-content>
      <form [formGroup]="form">
        <mat-tab-group [selectedIndex]="initialTab">
          <mat-tab label="Datos de empresa">
            <div class="tab-content">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nombre</mat-label>
                <input matInput formControlName="nombre">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Dirección</mat-label>
                <input matInput formControlName="direccion">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Código postal</mat-label>
                <input matInput formControlName="codigoPostal" placeholder="28001">
                <mat-error>Código postal obligatorio para facturación</mat-error>
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Provincia</mat-label>
                <input matInput formControlName="provincia" placeholder="Madrid">
                <mat-error>Provincia obligatoria para facturación</mat-error>
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>País</mat-label>
                <input matInput formControlName="pais" placeholder="España">
                <mat-error>País obligatorio para facturación</mat-error>
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>NIF/CIF</mat-label>
                <input matInput formControlName="nif" placeholder="12345678A">
                <mat-error>NIF/CIF no válido</mat-error>
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Teléfono</mat-label>
                <input matInput formControlName="telefono">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email</mat-label>
                <input matInput formControlName="email">
              </mat-form-field>
              <p class="hint">Logo en cabecera de PDF (PNG/JPEG/WebP, máx. ~380 KB).</p>
              @if (logoPreviewSrc) {
                <div class="firma-preview-wrap">
                  <img [src]="logoPreviewSrc" alt="Vista previa logo" class="logo-preview" />
                </div>
              }
              <input type="file" accept="image/png,image/jpeg,image/jpg,image/webp" #logoInput (change)="onLogoFile($event)" hidden />
              <button mat-stroked-button type="button" (click)="logoInput.click()">Subir logo</button>
              @if (logoPreviewSrc || tieneLogoGuardada) {
                <button mat-button type="button" color="warn" (click)="quitarLogo()">Quitar logo</button>
              }
            </div>
          </mat-tab>
          <mat-tab label="Firma en PDF">
            <div class="tab-content">
              <p class="hint">Imagen que aparecerá al final de presupuestos y facturas en PDF (PNG o JPEG, máx. ~400 KB).</p>
              @if (firmaPreviewSrc) {
                <div class="firma-preview-wrap">
                  <img [src]="firmaPreviewSrc" alt="Vista previa firma" class="firma-preview" />
                </div>
              }
              <input type="file" accept="image/png,image/jpeg,image/webp" #firmaInput (change)="onFirmaFile($event)" hidden />
              <button mat-stroked-button type="button" (click)="firmaInput.click()">Elegir imagen</button>
              @if (firmaPreviewSrc || tieneFirmaGuardada) {
                <button mat-button type="button" color="warn" (click)="quitarFirma()">Quitar firma</button>
              }
            </div>
          </mat-tab>
          <mat-tab label="Notas pie presupuesto">
            <div class="tab-content">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Notas al pie del presupuesto</mat-label>
                <textarea matInput formControlName="notasPiePresupuesto" rows="6"
                  placeholder="Texto que aparecerá al final del PDF del presupuesto"></textarea>
              </mat-form-field>
            </div>
          </mat-tab>
          <mat-tab label="Notas pie factura">
            <div class="tab-content">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Notas al pie de la factura</mat-label>
                <textarea matInput formControlName="notasPieFactura" rows="6"
                  placeholder="Texto que aparecerá al final del PDF de la factura"></textarea>
              </mat-form-field>
            </div>
          </mat-tab>
          <mat-tab label="Correo de envío">
            <div class="tab-content">
              <p class="hint">Correo desde el que se enviarán presupuestos y facturas por email.</p>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Servidor SMTP</mat-label>
                <input matInput formControlName="mailHost" placeholder="smtp.gmail.com">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Puerto</mat-label>
                <input matInput formControlName="mailPort" type="number" placeholder="587">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email (usuario)</mat-label>
                <input matInput formControlName="mailUsername" type="email" placeholder="tu@email.com">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Contraseña</mat-label>
                <input matInput formControlName="mailPassword" type="password"
                  placeholder="{{ mailConfigurado ? 'Dejar vacío para no cambiar' : 'Contraseña de aplicación' }}">
                <mat-hint>Gmail: use contraseña de aplicación si tiene 2FA</mat-hint>
              </mat-form-field>
            </div>
          </mat-tab>
        </mat-tab-group>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-raised-button color="primary" (click)="save()" [disabled]="form.invalid || saving">
        Guardar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .tab-content {
      padding: 16px 0;
    }
    .full-width {
      width: 100%;
      display: block;
      margin-bottom: 16px;
    }
    .hint {
      color: rgba(0,0,0,0.6);
      font-size: 14px;
      margin-bottom: 16px;
    }
    .firma-preview-wrap { margin: 12px 0; }
    .firma-preview { max-height: 100px; max-width: 200px; object-fit: contain; border: 1px solid #e0e0e0; border-radius: 4px; }
    .logo-preview { max-height: 72px; max-width: 200px; object-fit: contain; border: 1px solid #e0e0e0; border-radius: 4px; }
  `],
})
export class ConfigEmpresaDialogComponent implements OnInit {
  form: FormGroup;
  saving = false;
  initialTab = 0;
  mailConfigurado = false;
  firmaPreviewSrc: string | null = null;
  nuevaFirmaBase64: string | null = null;
  /** none | set | clear — control de envío de firma al guardar */
  firmaCambiada: 'none' | 'set' | 'clear' = 'none';
  tieneFirmaGuardada = false;
  logoPreviewSrc: string | null = null;
  logoCambiada: 'none' | 'set' | 'clear' = 'none';
  nuevaLogoBase64: string | null = null;
  tieneLogoGuardada = false;

  constructor(
    private fb: FormBuilder,
    private configService: ConfigService,
    private snackBar: MatSnackBar,
    public ref: MatDialogRef<ConfigEmpresaDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { context?: ConfigContext }
  ) {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      direccion: ['', Validators.required],
      codigoPostal: ['', Validators.required],
      provincia: ['', Validators.required],
      pais: ['España', Validators.required],
      nif: ['', [Validators.required, nifValidator()]],
      telefono: [''],
      email: [''],
      notasPiePresupuesto: [''],
      notasPieFactura: [''],
      mailHost: [''],
      mailPort: [587],
      mailUsername: [''],
      mailPassword: [''],
    });
    if (data?.context === 'factura') this.initialTab = 0;
    else if (data?.context === 'presupuesto') this.initialTab = 2;
    else if (data?.context === 'mail') this.initialTab = 4;
  }

  ngOnInit(): void {
    this.configService.getEmpresa().subscribe({
      next: (e: Empresa) => {
        this.mailConfigurado = e.mailConfigurado ?? false;
        this.tieneLogoGuardada = !!(e.tieneLogo && e.logoImagenBase64);
        this.logoPreviewSrc = dataUrlFromStoredBase64(e.logoImagenBase64 ?? null);
        this.logoCambiada = 'none';
        this.nuevaLogoBase64 = null;
        this.tieneFirmaGuardada = !!(e.tieneFirma && e.firmaImagenBase64);
        this.firmaPreviewSrc = dataUrlFromStoredBase64(e.firmaImagenBase64 ?? null);
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
        });
      },
      error: () => this.snackBar.open('Error al cargar configuración', 'Cerrar', { duration: 3000 }),
    });
  }

  onLogoFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !/^image\/(png|jpeg|jpg|webp)$/i.test(file.type)) {
      this.snackBar.open('Formato no admitido (PNG, JPEG o WebP)', 'Cerrar', { duration: 4000 });
      input.value = '';
      return;
    }
    if (file.size > 380000) {
      this.snackBar.open('El logo no puede superar ~380 KB', 'Cerrar', { duration: 4000 });
      input.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const r = reader.result as string;
      this.logoPreviewSrc = r;
      const comma = r.indexOf(',');
      this.nuevaLogoBase64 = comma >= 0 ? r.slice(comma + 1) : r;
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
  }

  onFirmaFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || file.size > 380000) {
      this.snackBar.open('Elige una imagen más pequeña (máx. ~380 KB)', 'Cerrar', { duration: 4000 });
      input.value = '';
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const r = reader.result as string;
      this.firmaPreviewSrc = r;
      const comma = r.indexOf(',');
      this.nuevaFirmaBase64 = comma >= 0 ? r.slice(comma + 1) : r;
      this.firmaCambiada = 'set';
      this.tieneFirmaGuardada = true;
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  quitarFirma(): void {
    this.firmaPreviewSrc = null;
    this.nuevaFirmaBase64 = null;
    this.firmaCambiada = 'clear';
    this.tieneFirmaGuardada = false;
  }

  save(): void {
    this.saving = true;
    const val = this.form.value;
    const payload: Record<string, unknown> = { ...val };
    if (!val.mailPassword?.trim()) delete payload['mailPassword'];
    if (this.firmaCambiada === 'clear') {
      payload['firmaImagenBase64'] = '';
    } else if (this.firmaCambiada === 'set' && this.nuevaFirmaBase64) {
      payload['firmaImagenBase64'] = this.nuevaFirmaBase64;
    }
    if (this.logoCambiada === 'clear') {
      payload['logoImagenBase64'] = '';
    } else if (this.logoCambiada === 'set' && this.nuevaLogoBase64) {
      payload['logoImagenBase64'] = this.nuevaLogoBase64;
    }
    this.configService.saveEmpresa(payload).subscribe({
      next: () => {
        this.snackBar.open('Configuración guardada', 'Cerrar', { duration: 3000 });
        this.ref.close(true);
      },
      error: () => {
        this.saving = false;
        this.snackBar.open('Error al guardar', 'Cerrar', { duration: 3000 });
      },
    });
  }
}
