import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Presupuesto } from '../../core/models/presupuesto.model';

export interface ImportarPresupuestoDialogData {
  presupuestos: Presupuesto[];
}

@Component({
  selector: 'app-importar-presupuesto-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatTableModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>Importar presupuesto como factura</h2>
    <mat-dialog-content>
      <p class="info">Selecciona un presupuesto pendiente para convertirlo en factura. El presupuesto pasará a estado "Aceptado".</p>
      @if (data.presupuestos.length === 0) {
        <p class="empty">No hay presupuestos pendientes para importar.</p>
      } @else {
        <table mat-table [dataSource]="dataSource" class="full-width">
          <ng-container matColumnDef="clienteNombre">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let row">{{ row.clienteNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="fechaCreacion">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let row">{{ row.fechaCreacion | date:'short' }}</td>
          </ng-container>
          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef>Total</th>
            <td mat-cell *matCellDef="let row">{{ row.total | number:'1.2-2' }} €</td>
          </ng-container>
          <ng-container matColumnDef="select">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let row">
              <button mat-raised-button color="primary" (click)="select(row)">Crear factura</button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .info {
      margin-bottom: 16px;
      color: rgba(0,0,0,0.6);
    }
    .empty {
      padding: 24px;
      text-align: center;
      color: rgba(0,0,0,0.6);
    }
    .full-width {
      width: 100%;
      min-width: 400px;
    }
  `],
})
export class ImportarPresupuestoDialogComponent {
  displayedColumns = ['clienteNombre', 'fechaCreacion', 'total', 'select'];
  dataSource: MatTableDataSource<Presupuesto>;

  constructor(
    public ref: MatDialogRef<ImportarPresupuestoDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ImportarPresupuestoDialogData
  ) {
    this.dataSource = new MatTableDataSource(data.presupuestos);
  }

  select(presupuesto: Presupuesto): void {
    this.ref.close(presupuesto);
  }
}
