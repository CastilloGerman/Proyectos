import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { Presupuesto } from '../../../core/models/presupuesto.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-presupuesto-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
  template: `
    <div class="presupuesto-list">
      <div class="header">
        <h1>Presupuestos</h1>
        <a mat-raised-button color="primary" routerLink="/presupuestos/nuevo">
          <mat-icon>add</mat-icon>
          Nuevo presupuesto
        </a>
      </div>
      <div class="table-container">
        <table mat-table [dataSource]="dataSource" class="full-width">
          <ng-container matColumnDef="clienteNombre">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let row">{{ row.clienteNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="fechaCreacion">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let row">{{ row.fechaCreacion | date:'short' }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let row">
              <mat-chip [color]="getEstadoColor(row.estado)">{{ row.estado }}</mat-chip>
            </td>
          </ng-container>
          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef>Total</th>
            <td mat-cell *matCellDef="let row">{{ row.total | number:'1.2-2' }} €</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let row">
              <button mat-icon-button [routerLink]="['/presupuestos', row.id]" matTooltip="Editar">
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
            <td class="mat-cell" colspan="5">No hay presupuestos</td>
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
export class PresupuestoListComponent implements OnInit {
  displayedColumns = ['clienteNombre', 'fechaCreacion', 'estado', 'total', 'actions'];
  dataSource = new MatTableDataSource<Presupuesto>([]);

  constructor(
    private presupuestoService: PresupuestoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.presupuestoService.getAll().subscribe((data) => {
      this.dataSource.data = data;
    });
  }

  delete(presupuesto: Presupuesto): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar presupuesto',
        message: `¿Eliminar presupuesto de ${presupuesto.clienteNombre}?`,
      },
    });
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.presupuestoService.delete(presupuesto.id).subscribe({
          next: () => {
            this.snackBar.open('Presupuesto eliminado', 'Cerrar', { duration: 3000 });
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
    if (estado === 'Aceptado') return 'primary';
    if (estado === 'Rechazado') return 'warn';
    return undefined;
  }
}
