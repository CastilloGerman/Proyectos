import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FacturaService } from '../../../core/services/factura.service';
import { Factura } from '../../../core/models/factura.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-factura-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
  template: `
    <div class="factura-list">
      <div class="header">
        <h1>Facturas</h1>
        <a mat-raised-button color="primary" routerLink="/facturas/nuevo">
          <mat-icon>add</mat-icon>
          Nueva factura
        </a>
      </div>
      <div class="table-container">
        <table mat-table [dataSource]="dataSource" class="full-width">
          <ng-container matColumnDef="numeroFactura">
            <th mat-header-cell *matHeaderCellDef>Nº Factura</th>
            <td mat-cell *matCellDef="let row">{{ row.numeroFactura }}</td>
          </ng-container>
          <ng-container matColumnDef="clienteNombre">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let row">{{ row.clienteNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="fechaCreacion">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let row">{{ row.fechaCreacion | date:'short' }}</td>
          </ng-container>
          <ng-container matColumnDef="estadoPago">
            <th mat-header-cell *matHeaderCellDef>Estado pago</th>
            <td mat-cell *matCellDef="let row">
              <mat-chip [color]="getEstadoColor(row.estadoPago)">{{ row.estadoPago }}</mat-chip>
            </td>
          </ng-container>
          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef>Total</th>
            <td mat-cell *matCellDef="let row">{{ row.total | number:'1.2-2' }} €</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let row">
              <button mat-icon-button [routerLink]="['/facturas', row.id]" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="delete(row)" matTooltip="Eliminar">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell" colspan="6">No hay facturas</td>
          </tr>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .table-container {
      overflow-x: auto;
    }

    .full-width {
      width: 100%;
    }
  `],
})
export class FacturaListComponent implements OnInit {
  displayedColumns = ['numeroFactura', 'clienteNombre', 'fechaCreacion', 'estadoPago', 'total', 'actions'];
  dataSource = new MatTableDataSource<Factura>([]);

  constructor(
    private facturaService: FacturaService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.facturaService.getAll().subscribe((data) => {
      this.dataSource.data = data;
    });
  }

  delete(factura: Factura): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar factura',
        message: `¿Eliminar factura ${factura.numeroFactura}?`,
      },
    });
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.facturaService.delete(factura.id).subscribe({
          next: () => {
            this.snackBar.open('Factura eliminada', 'Cerrar', { duration: 3000 });
            this.load();
          },
          error: () => {
            this.snackBar.open('Error al eliminar', 'Cerrar', { duration: 3000 });
          },
        });
      }
    });
  }

  getEstadoColor(estado: string): 'primary' | 'accent' | 'warn' | undefined {
    if (estado === 'Pagada') return 'primary';
    if (estado === 'Parcial') return 'accent';
    return undefined;
  }
}
