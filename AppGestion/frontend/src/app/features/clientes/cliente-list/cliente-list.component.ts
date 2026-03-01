import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/auth/auth.service';
import { ClienteService } from '../../../core/services/cliente.service';
import { Cliente } from '../../../core/models/cliente.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-cliente-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
  template: `
    <div class="cliente-list">
      <div class="header">
        <h1>Clientes</h1>
        @if (auth.canWrite()) {
        <a mat-raised-button color="primary" routerLink="/clientes/nuevo">
          <mat-icon>add</mat-icon>
          Nuevo cliente
        </a>
        }
      </div>
      <div class="table-container">
        <table mat-table [dataSource]="dataSource" class="full-width">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let row">{{ row.nombre }}</td>
          </ng-container>
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let row">{{ row.email }}</td>
          </ng-container>
          <ng-container matColumnDef="telefono">
            <th mat-header-cell *matHeaderCellDef>Teléfono</th>
            <td mat-cell *matCellDef="let row">{{ row.telefono }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let row">
              @if (auth.canWrite()) {
              <button mat-icon-button [routerLink]="['/clientes', row.id]" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="delete(row)" matTooltip="Eliminar">
                <mat-icon>delete</mat-icon>
              </button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell" colspan="4">No hay clientes. Crea uno para empezar.</td>
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
export class ClienteListComponent implements OnInit {
  displayedColumns = ['nombre', 'email', 'telefono', 'actions'];
  dataSource = new MatTableDataSource<Cliente>([]);

  constructor(
    public auth: AuthService,
    private clienteService: ClienteService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.clienteService.getAll().subscribe((data) => {
      this.dataSource.data = data;
    });
  }

  delete(cliente: Cliente): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar cliente',
        message: `¿Eliminar a ${cliente.nombre}?`,
      },
    });
    ref.afterClosed().subscribe((ok) => {
      if (ok) {
        this.clienteService.delete(cliente.id).subscribe({
          next: () => {
            this.snackBar.open('Cliente eliminado', 'Cerrar', { duration: 3000 });
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
