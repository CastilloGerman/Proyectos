import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MaterialService } from '../../../core/services/material.service';
import { Material } from '../../../core/models/material.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-material-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
  template: `
    <div class="material-list">
      <div class="header">
        <h1>Materiales</h1>
        <a mat-raised-button color="primary" routerLink="/materiales/nuevo">
          <mat-icon>add</mat-icon>
          Nuevo material
        </a>
      </div>
      <div class="table-container">
        <table mat-table [dataSource]="dataSource" class="full-width">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let row">{{ row.nombre }}</td>
          </ng-container>
          <ng-container matColumnDef="precioUnitario">
            <th mat-header-cell *matHeaderCellDef>Precio unitario</th>
            <td mat-cell *matCellDef="let row">{{ row.precioUnitario | number:'1.2-2' }} €</td>
          </ng-container>
          <ng-container matColumnDef="unidadMedida">
            <th mat-header-cell *matHeaderCellDef>Unidad</th>
            <td mat-cell *matCellDef="let row">{{ row.unidadMedida || 'ud' }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let row">
              <button mat-icon-button [routerLink]="['/materiales', row.id]" matTooltip="Editar">
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
            <td class="mat-cell" colspan="4">No hay materiales. Crea uno para empezar.</td>
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
export class MaterialListComponent implements OnInit {
  displayedColumns = ['nombre', 'precioUnitario', 'unidadMedida', 'actions'];
  dataSource = new MatTableDataSource<Material>([]);

  constructor(
    private materialService: MaterialService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.materialService.getAll().subscribe((data) => {
      this.dataSource.data = data;
    });
  }

  delete(material: Material): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar material',
        message: `¿Eliminar "${material.nombre}"?`,
      },
    });
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.materialService.delete(material.id).subscribe({
          next: () => {
            this.snackBar.open('Material eliminado', 'Cerrar', { duration: 3000 });
            this.load();
          },
          error: () => {
            this.snackBar.open('Error al eliminar', 'Cerrar', { duration: 3000 });
          },
        });
      }
    });
  }
}
