import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
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
import { ConfigEmpresaDialogComponent } from '../../../shared/config-empresa-dialog/config-empresa-dialog.component';
import { EnviarEmailDialogComponent } from '../../../shared/enviar-email-dialog/enviar-email-dialog.component';

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
        <div class="header-actions">
          <button mat-icon-button (click)="openConfig()" matTooltip="Configuración plantillas">
            <mat-icon>settings</mat-icon>
          </button>
          <a mat-raised-button color="primary" routerLink="/presupuestos/nuevo">
            <mat-icon>add</mat-icon>
            Nuevo presupuesto
          </a>
        </div>
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
              @if (row.estado === 'Pendiente') {
                <button mat-icon-button (click)="crearFactura(row)" matTooltip="Crear factura">
                  <mat-icon>receipt</mat-icon>
                </button>
              }
              <button mat-icon-button (click)="enviarEmail(row)" matTooltip="Enviar por email">
                <mat-icon>email</mat-icon>
              </button>
              <button mat-icon-button (click)="downloadPdf(row)" matTooltip="Descargar PDF">
                <mat-icon>picture_as_pdf</mat-icon>
              </button>
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
export class PresupuestoListComponent implements OnInit {
  displayedColumns = ['clienteNombre', 'fechaCreacion', 'estado', 'total', 'actions'];
  dataSource = new MatTableDataSource<Presupuesto>([]);

  constructor(
    private presupuestoService: PresupuestoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.presupuestoService.getAll().subscribe({
      next: (data) => this.dataSource.data = data,
      error: (err) => {
        const msg = err.status === 403
          ? 'Acceso denegado. Verifica que la API esté en modo desarrollo (perfil local).'
          : 'Error al cargar presupuestos';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
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

  openConfig(): void {
    this.dialog.open(ConfigEmpresaDialogComponent, {
      width: '500px',
      data: { context: 'presupuesto' },
    });
  }

  crearFactura(presupuesto: Presupuesto): void {
    this.presupuestoService.createFacturaFromPresupuesto(presupuesto.id).subscribe({
      next: (factura) => {
        this.snackBar.open('Factura creada. El presupuesto ha pasado a estado Aceptado.', 'Cerrar', { duration: 4000 });
        this.load();
        this.router.navigate(['/facturas', factura.id]);
      },
      error: (err) => {
        this.snackBar.open(err.error?.message || 'Error al crear factura', 'Cerrar', { duration: 4000 });
      },
    });
  }

  enviarEmail(presupuesto: Presupuesto): void {
    const ref = this.dialog.open(EnviarEmailDialogComponent, {
      width: '400px',
      data: {
        titulo: 'Enviar presupuesto por email',
        emailCliente: presupuesto.clienteEmail || undefined,
      },
    });
    ref.afterClosed().subscribe((email: string | undefined) => {
      if (email !== undefined) {
        this.presupuestoService.enviarPorEmail(presupuesto.id, email || undefined).subscribe({
          next: () => {
            this.snackBar.open('Presupuesto enviado por email correctamente', 'Cerrar', { duration: 3000 });
          },
          error: (err) => {
            const msg = err.error?.detail ?? err.error?.message ?? 'Error al enviar el email';
            this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
          },
        });
      }
    });
  }

  downloadPdf(presupuesto: Presupuesto): void {
    this.presupuestoService.downloadPdf(presupuesto.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `presupuesto-${presupuesto.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.snackBar.open('Error al descargar PDF', 'Cerrar', { duration: 3000 }),
    });
  }
}
