import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FacturaService } from '../../../core/services/factura.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { Factura } from '../../../core/models/factura.model';
import { Presupuesto } from '../../../core/models/presupuesto.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ImportarPresupuestoDialogComponent } from '../../../shared/importar-presupuesto-dialog/importar-presupuesto-dialog.component';
import { ConfigEmpresaDialogComponent } from '../../../shared/config-empresa-dialog/config-empresa-dialog.component';
import { EnviarEmailDialogComponent } from '../../../shared/enviar-email-dialog/enviar-email-dialog.component';

@Component({
  selector: 'app-factura-list',
  standalone: true,
  imports: [
    CommonModule,
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
        <div class="header-actions">
          <button mat-icon-button (click)="openConfig()" matTooltip="Configuración plantillas">
            <mat-icon>settings</mat-icon>
          </button>
          <button mat-stroked-button (click)="openImportarPresupuesto()">
            <mat-icon>file_download</mat-icon>
            Importar presupuesto
          </button>
          <a mat-raised-button color="primary" routerLink="/facturas/nuevo">
            <mat-icon>add</mat-icon>
            Nueva factura
          </a>
        </div>
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
              <button mat-icon-button (click)="enviarEmail(row)" matTooltip="Enviar por email">
                <mat-icon>email</mat-icon>
              </button>
              <button mat-icon-button (click)="downloadPdf(row)" matTooltip="Descargar PDF">
                <mat-icon>picture_as_pdf</mat-icon>
              </button>
              <a mat-icon-button [routerLink]="['/facturas', row.id]" matTooltip="Editar">
                <mat-icon>edit</mat-icon>
              </a>
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

    .header-actions {
      display: flex;
      gap: 8px;
      align-items: center;
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
    private presupuestoService: PresupuestoService,
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

  openImportarPresupuesto(): void {
    this.presupuestoService.getAll().subscribe({
      next: (presupuestos) => {
        const pendientes = presupuestos.filter((p) => p.estado === 'Pendiente');
        const ref = this.dialog.open(ImportarPresupuestoDialogComponent, {
          width: '600px',
          data: { presupuestos: pendientes },
        });
        ref.afterClosed().subscribe((selected: Presupuesto | undefined) => {
          if (selected) {
            this.presupuestoService.createFacturaFromPresupuesto(selected.id).subscribe({
              next: (factura) => {
                this.snackBar.open('Factura creada desde presupuesto', 'Cerrar', { duration: 3000 });
                this.facturaService.getAll().subscribe((data) => {
                  this.dataSource.data = data;
                });
              },
              error: (err) => {
                this.snackBar.open(err.error?.message || 'Error al crear factura', 'Cerrar', { duration: 4000 });
              },
            });
          }
        });
      },
      error: () => this.snackBar.open('Error al cargar presupuestos', 'Cerrar', { duration: 3000 }),
    });
  }

  openConfig(): void {
    this.dialog.open(ConfigEmpresaDialogComponent, {
      width: '500px',
      data: { context: 'factura' },
    });
  }

  enviarEmail(factura: Factura): void {
    const ref = this.dialog.open(EnviarEmailDialogComponent, {
      width: '400px',
      data: {
        titulo: 'Enviar factura por email',
        emailCliente: factura.clienteEmail || undefined,
      },
    });
    ref.afterClosed().subscribe((email: string | undefined) => {
      if (email !== undefined) {
        this.facturaService.enviarPorEmail(factura.id, email || undefined).subscribe({
          next: () => {
            this.snackBar.open('Factura enviada por email correctamente', 'Cerrar', { duration: 3000 });
          },
          error: (err) => {
            const msg = err.error?.detail ?? err.error?.message ?? 'Error al enviar el email';
            const needsConfig = msg.includes('Configure') || msg.includes('correo de envío');
            this.snackBar.open(msg, needsConfig ? 'Configurar' : 'Cerrar', { duration: needsConfig ? 8000 : 5000 })
              .onAction().subscribe(() => {
                if (needsConfig) {
                  this.dialog.open(ConfigEmpresaDialogComponent, { width: '500px', data: { context: 'mail' } });
                }
              });
          },
        });
      }
    });
  }

  downloadPdf(factura: Factura): void {
    this.facturaService.downloadPdf(factura.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `factura-${factura.numeroFactura || factura.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.snackBar.open('Error al descargar PDF', 'Cerrar', { duration: 3000 }),
    });
  }
}
