import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-anular-factura-dialog',
  imports: [MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, FormsModule],
  template: `
    <h2 mat-dialog-title>Anular factura</h2>
    <mat-dialog-content>
      <p class="hint">
        La factura quedará marcada como anulada; el número se conserva en la serie correlativa. No se elimina de la base de
        datos.
      </p>
      <mat-form-field appearance="outline" class="full">
        <mat-label>Motivo (opcional)</mat-label>
        <textarea matInput rows="3" maxlength="255" [(ngModel)]="motivo"></textarea>
        <mat-hint align="end">{{ motivo.length }}/255</mat-hint>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-button color="warn" [mat-dialog-close]="motivo.trim() || null">Anular</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .hint {
        margin: 0 0 12px;
        color: var(--app-text-secondary, #64748b);
        font-size: 0.9rem;
      }
      .full {
        width: 100%;
      }
    `,
  ],
})
export class AnularFacturaDialogComponent {
  motivo = '';
}
