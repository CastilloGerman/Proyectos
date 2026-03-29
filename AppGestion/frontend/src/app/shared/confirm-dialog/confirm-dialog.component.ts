import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

export interface ConfirmDialogData {
  title: string;
  message: string;
  /** Texto del botón de confirmación (por defecto «Eliminar»). */
  confirmLabel?: string;
  /** Color Material del botón de confirmación (por defecto warn). */
  confirmColor?: 'primary' | 'accent' | 'warn';
}

@Component({
    selector: 'app-confirm-dialog',
    imports: [MatDialogModule, MatButtonModule],
    template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content class="dialog-msg">{{ data.message }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-button [color]="data.confirmColor || 'warn'" [mat-dialog-close]="true">{{ data.confirmLabel || 'Eliminar' }}</button>
    </mat-dialog-actions>
  `,
    styles: [`
    .dialog-msg { white-space: pre-line; }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    public ref: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}
}
