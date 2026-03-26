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
        @if (auth.canMutate()) {
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
          <ng-container matColumnDef="estadoPanel">
            <th mat-header-cell *matHeaderCellDef>Cobros y documentos</th>
            <td mat-cell *matCellDef="let row">
              <a
                mat-stroked-button
                color="primary"
                [routerLink]="['/clientes', row.id, 'panel']"
                class="btn-ver-estado"
              >
                <mat-icon>insights</mat-icon>
                Ver estado
              </a>
            </td>
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
              @if (auth.canMutate()) {
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
          <tr class="mat-row empty-row" *matNoDataRow>
            <td class="mat-cell" colspan="5">
              <div class="empty-state">
                <mat-icon class="empty-illus" aria-hidden="true">groups</mat-icon>
                <p class="empty-title">Aún no tienes clientes</p>
                <p class="empty-text">Crea el primero para vincular presupuestos y facturas.</p>
                @if (auth.canMutate()) {
                <a mat-stroked-button color="primary" routerLink="/clientes/nuevo">Nuevo cliente</a>
                }
              </div>
            </td>
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

    .btn-ver-estado mat-icon {
      margin-right: 4px;
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .empty-row .mat-mdc-cell {
      border-bottom: none;
      padding: 48px 24px;
    }

    .empty-state {
      text-align: center;
      max-width: 360px;
      margin: 0 auto;
      color: var(--app-text-secondary, #64748b);
    }

    .empty-illus {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: var(--app-accent-copper, #b87333);
      opacity: 0.85;
    }

    .empty-title {
      margin: 16px 0 8px;
      font-size: 1.125rem;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
      font-family: 'Sora', 'IBM Plex Sans', sans-serif;
    }

    .empty-text {
      margin: 0 0 20px;
      font-size: 0.9375rem;
      line-height: 1.5;
    }
  `],
})
export class ClienteListComponent implements OnInit {
  displayedColumns = ['nombre', 'estadoPanel', 'email', 'telefono', 'actions'];
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
