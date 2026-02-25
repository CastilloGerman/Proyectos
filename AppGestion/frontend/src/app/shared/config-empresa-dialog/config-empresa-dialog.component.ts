import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ConfigService } from '../../core/services/config.service';
import { Empresa } from '../../core/models/empresa.model';

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
                <mat-label>NIF/CIF</mat-label>
                <input matInput formControlName="nif">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Teléfono</mat-label>
                <input matInput formControlName="telefono">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Email</mat-label>
                <input matInput formControlName="email">
              </mat-form-field>
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
  `],
})
export class ConfigEmpresaDialogComponent implements OnInit {
  form: FormGroup;
  saving = false;
  initialTab = 0;
  mailConfigurado = false;

  constructor(
    private fb: FormBuilder,
    private configService: ConfigService,
    private snackBar: MatSnackBar,
    public ref: MatDialogRef<ConfigEmpresaDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { context?: ConfigContext }
  ) {
    this.form = this.fb.group({
      nombre: [''],
      direccion: [''],
      nif: [''],
      telefono: [''],
      email: [''],
      notasPiePresupuesto: [''],
      notasPieFactura: [''],
      mailHost: [''],
      mailPort: [587],
      mailUsername: [''],
      mailPassword: [''],
    });
    if (data?.context === 'factura') this.initialTab = 2;
    else if (data?.context === 'presupuesto') this.initialTab = 1;
    else if (data?.context === 'mail') this.initialTab = 3;
  }

  ngOnInit(): void {
    this.configService.getEmpresa().subscribe({
      next: (e: Empresa) => {
        this.mailConfigurado = e.mailConfigurado ?? false;
        this.form.patchValue({
          nombre: e.nombre ?? '',
          direccion: e.direccion ?? '',
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

  save(): void {
    this.saving = true;
    const val = this.form.value;
    const payload = { ...val };
    if (!val.mailPassword?.trim()) delete payload.mailPassword;
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
