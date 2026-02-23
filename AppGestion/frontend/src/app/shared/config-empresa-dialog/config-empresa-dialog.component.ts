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

export type ConfigContext = 'presupuesto' | 'factura';

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
  `],
})
export class ConfigEmpresaDialogComponent implements OnInit {
  form: FormGroup;
  saving = false;
  initialTab = 0;

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
    });
    if (data?.context === 'factura') this.initialTab = 2;
    else if (data?.context === 'presupuesto') this.initialTab = 1;
  }

  ngOnInit(): void {
    this.configService.getEmpresa().subscribe({
      next: (e: Empresa) => this.form.patchValue({
        nombre: e.nombre ?? '',
        direccion: e.direccion ?? '',
        nif: e.nif ?? '',
        telefono: e.telefono ?? '',
        email: e.email ?? '',
        notasPiePresupuesto: e.notasPiePresupuesto ?? '',
        notasPieFactura: e.notasPieFactura ?? '',
      }),
      error: () => this.snackBar.open('Error al cargar configuración', 'Cerrar', { duration: 3000 }),
    });
  }

  save(): void {
    this.saving = true;
    this.configService.saveEmpresa(this.form.value).subscribe({
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
