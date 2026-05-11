import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface FacturaParcialImporteDialogData {
  totalFactura: number;
  numeroFactura?: string;
  clienteNombre?: string;
  /** Valor inicial (ej. último cobrado declarado si existía). */
  importeSugerido?: number | null;
}

@Component({
  selector: 'app-factura-parcial-importe-dialog',
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Importe cobrado parcial</h2>
    <mat-dialog-content>
      <p class="contexto">
        @if (data.clienteNombre) {
          {{ data.clienteNombre }}
          @if (data.numeroFactura) {
            <span class="muted"> · {{ data.numeroFactura }}</span>
          }
        } @else if (data.numeroFactura) {
          <span class="muted">{{ data.numeroFactura }}</span>
        }
      </p>
      <p class="totales">
        Total factura: <strong>{{ data.totalFactura | number:'1.2-2' }} €</strong>
      </p>
      <mat-form-field appearance="outline" class="full">
        <mat-label>Importe cobrado hasta ahora</mat-label>
        <input
          matInput
          type="number"
          name="imp"
          [(ngModel)]="importe"
          min="0.01"
          step="0.01"
          (keydown.enter)="confirmar()"
        />
        <mat-hint>Debe ser mayor que cero y no superar el total</mat-hint>
      </mat-form-field>
      @if (mensajeError) {
        <p class="error">{{ mensajeError }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button type="button" mat-button mat-dialog-close>Cancelar</button>
      <button type="button" mat-flat-button color="primary" (click)="confirmar()">Aceptar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .contexto { margin: 0 0 8px; font-size: 0.92rem; }
    .muted { color: var(--app-text-secondary, #64748b); }
    .totales { margin: 0 0 16px; font-size: 0.92rem; }
    .full { width: 100%; }
    .error { margin: 8px 0 0; color: var(--mat-form-field-error-text-color, #b91c1c); font-size: 0.85rem; }
  `,
})
export class FacturaParcialImporteDialogComponent {
  importe: number | null;

  mensajeError = '';

  constructor(
    @Inject(MAT_DIALOG_DATA) readonly data: FacturaParcialImporteDialogData,
    private readonly dialogRef: MatDialogRef<FacturaParcialImporteDialogComponent, number | undefined>,
  ) {
    const sug = data.importeSugerido;
    this.importe = sug != null && sug > 0 ? Math.round(sug * 100) / 100 : null;
  }

  confirmar(): void {
    this.mensajeError = '';
    const total = +(this.data.totalFactura ?? 0);
    if (!Number.isFinite(total) || total <= 0) {
      this.mensajeError = 'Total de factura no válido';
      return;
    }
    const raw = this.importe;
    if (raw === null || raw === undefined || String(raw).trim() === '') {
      this.mensajeError = 'Indica el importe cobrado';
      return;
    }
    const v = typeof raw === 'number' ? raw : parseFloat(String(raw).replace(',', '.'));
    if (!Number.isFinite(v) || v <= 0) {
      this.mensajeError = 'El importe debe ser mayor que cero';
      return;
    }
    if (v > total + 0.009) {
      this.mensajeError = `No puede superar el total (${total.toFixed(2)} €)`;
      return;
    }
    if (v >= total - 0.009) {
      this.mensajeError = 'Si está cobrada por completo, elige el estado «Pagada»';
      return;
    }
    this.dialogRef.close(Math.round(v * 100) / 100);
  }
}
