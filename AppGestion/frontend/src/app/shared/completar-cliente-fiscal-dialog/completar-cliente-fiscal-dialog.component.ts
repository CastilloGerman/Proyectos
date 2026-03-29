import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ClienteService } from '../../core/services/cliente.service';

export interface CompletarClienteFiscalDialogData {
  clienteId: number;
  clienteNombre: string;
}

@Component({
  selector: 'app-completar-cliente-fiscal-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>Datos fiscales del cliente</h2>
    <mat-dialog-content>
      <p class="intro">
        Para facturar a <strong>{{ data.clienteNombre }}</strong> necesitas completar su información fiscal.
      </p>
      <form [formGroup]="form" (ngSubmit)="guardar()">
        <mat-form-field appearance="outline" class="full">
          <mat-label>DNI / NIF</mat-label>
          <input matInput formControlName="dni" autocomplete="off" />
          @if (form.get('dni')?.hasError('required') && form.get('dni')?.touched) {
            <mat-error>Obligatorio</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Dirección</mat-label>
          <input matInput formControlName="direccion" />
          @if (form.get('direccion')?.hasError('required') && form.get('direccion')?.touched) {
            <mat-error>Obligatorio</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Código postal</mat-label>
          <input matInput formControlName="codigoPostal" />
          @if (form.get('codigoPostal')?.hasError('required') && form.get('codigoPostal')?.touched) {
            <mat-error>Obligatorio</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Provincia</mat-label>
          <input matInput formControlName="provincia" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>País</mat-label>
          <input matInput formControlName="pais" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Teléfono</mat-label>
          <input matInput formControlName="telefono" />
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" (click)="cancelar()">Cancelar</button>
      <button mat-flat-button color="primary" type="button" (click)="guardar()" [disabled]="form.invalid || saving">
        Guardar y continuar con la factura
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .intro {
        margin: 0 0 16px;
        font-size: 0.9375rem;
        line-height: 1.5;
        color: var(--app-text-secondary, #64748b);
      }
      .full {
        width: 100%;
        display: block;
      }
      mat-dialog-content {
        min-width: 320px;
        max-width: 480px;
        padding-top: 8px;
      }
    `,
  ],
})
export class CompletarClienteFiscalDialogComponent {
  saving = false;
  form = this.fb.group({
    dni: ['', Validators.required],
    direccion: ['', Validators.required],
    codigoPostal: ['', Validators.required],
    provincia: [''],
    pais: ['España'],
    telefono: [''],
    email: [''],
  });

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CompletarClienteFiscalDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: CompletarClienteFiscalDialogData,
    private clienteService: ClienteService,
    private snackBar: MatSnackBar
  ) {}

  cancelar(): void {
    this.dialogRef.close(false);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    const v = this.form.getRawValue();
    this.clienteService
      .completar(this.data.clienteId, {
        dni: v.dni!.trim(),
        direccion: v.direccion!.trim(),
        codigoPostal: v.codigoPostal!.trim(),
        provincia: v.provincia?.trim() || undefined,
        pais: v.pais?.trim() || undefined,
        telefono: v.telefono?.trim() || undefined,
        email: v.email?.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.dialogRef.close(true);
        },
        error: (err) => {
          this.saving = false;
          const msg =
            err.error?.message ||
            err.error?.detail ||
            (typeof err.error === 'string' ? err.error : null) ||
            'No se pudieron guardar los datos';
          this.snackBar.open(msg, 'Cerrar', { duration: 6000 });
        },
      });
  }
}
