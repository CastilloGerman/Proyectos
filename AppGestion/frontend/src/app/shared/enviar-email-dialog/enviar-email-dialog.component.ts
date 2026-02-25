import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface EnviarEmailDialogData {
  titulo: string;
  emailCliente?: string;
}

@Component({
  selector: 'app-enviar-email-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.titulo }}</h2>
    <mat-dialog-content>
      @if (data.emailCliente) {
        <p class="info">Se enviará al email del cliente: <strong>{{ data.emailCliente }}</strong></p>
      } @else {
        <p class="info">El cliente no tiene email registrado. Indica el email de destino:</p>
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Email</mat-label>
          <input matInput type="email" [formControl]="emailControl" placeholder="cliente@ejemplo.com">
          <mat-error>Introduce un email válido</mat-error>
        </mat-form-field>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-raised-button color="primary" [mat-dialog-close]="getEmail()" [disabled]="!isValid()">
        Enviar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .info {
      margin-bottom: 16px;
      color: rgba(0, 0, 0, 0.7);
    }
    .full-width {
      width: 100%;
      min-width: 280px;
    }
  `],
})
export class EnviarEmailDialogComponent {
  emailControl = new FormControl('', [Validators.required, Validators.email]);

  constructor(
    public ref: MatDialogRef<EnviarEmailDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EnviarEmailDialogData
  ) {}

  isValid(): boolean {
    if (this.data.emailCliente) return true;
    return this.emailControl.valid ?? false;
  }

  getEmail(): string | undefined {
    if (this.data.emailCliente) return this.data.emailCliente;
    return this.emailControl.value?.trim() || undefined;
  }
}
