import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { trigger, transition, query, stagger, animate, style } from '@angular/animations';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EstadoBadgeComponent } from '../../../shared/estado-badge/estado-badge.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { AuthService } from '../../../core/auth/auth.service';
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
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSortModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    EstadoBadgeComponent,
    SkeletonComponent,
  ],
  animations: [
    trigger('listAnimation', [
      transition(':enter', [
        query('tr.mat-row', [
          style({ opacity: 0, transform: 'translateY(12px)' }),
          stagger('50ms', [
            animate('200ms ease-out', style({ opacity: 1, transform: 'translateY(0)' })),
          ]),
        ], { optional: true }),
      ]),
    ]),
  ],
  template: `
    <div class="factura-list">
      <div class="header">
        <h1>Facturas</h1>
        <div class="header-actions">
          <button mat-icon-button (click)="openConfig()" matTooltip="Textos al final del PDF">
            <mat-icon>settings</mat-icon>
          </button>
          @if (auth.canMutate()) {
          <button mat-stroked-button (click)="openImportarPresupuesto()">
            <mat-icon>file_download</mat-icon>
            Importar presupuesto
          </button>
          <a mat-raised-button color="primary" routerLink="/facturas/nuevo">
            <mat-icon>add</mat-icon>
            Nueva factura
          </a>
          }
        </div>
      </div>
      <div class="filters-bar">
        <mat-form-field appearance="outline" class="filter-text">
          <mat-label>Buscar</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput (input)="applyTextFilter($event)" placeholder="Nº factura, cliente…">
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-estado">
          <mat-label>Estado pago</mat-label>
          <mat-select (selectionChange)="applyEstadoFilter($event.value)" value="">
            <mat-option value="">Todos</mat-option>
            <mat-option value="No Pagada">No Pagada</mat-option>
            <mat-option value="Parcial">Parcial</mat-option>
            <mat-option value="Pagada">Pagada</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      @if (isLoading) {
        <app-skeleton [rows]="10"></app-skeleton>
      } @else {
      <div class="invoice-card" [@listAnimation]>
        <div class="table-container">
        <table mat-table [dataSource]="dataSource" matSort class="full-width">
          <ng-container matColumnDef="numeroFactura">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Nº Factura</th>
            <td mat-cell *matCellDef="let row">{{ row.numeroFactura }}</td>
          </ng-container>
          <ng-container matColumnDef="clienteNombre">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Cliente</th>
            <td mat-cell *matCellDef="let row">{{ row.clienteNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="fechaCreacion">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Fecha</th>
            <td mat-cell *matCellDef="let row">{{ row.fechaCreacion | date:'dd/MM/yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="fechaVencimiento">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Vencimiento</th>
            <td mat-cell *matCellDef="let row">
              @if (row.fechaVencimiento) {
                <span [class]="getVencimientoClass(row)">{{ row.fechaVencimiento | date:'dd/MM/yyyy' }}</span>
              } @else {
                <span class="text-muted">—</span>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="estadoPago">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Estado</th>
            <td mat-cell *matCellDef="let row">
              <app-estado-badge [estado]="row.estadoPago"></app-estado-badge>
            </td>
          </ng-container>
          <ng-container matColumnDef="total">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Total</th>
            <td mat-cell *matCellDef="let row" class="text-right">{{ row.total | number:'1.2-2' }} €</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Acciones</th>
            <td mat-cell *matCellDef="let row" class="actions-cell">
              @if (auth.canMutate()) {
              <a mat-stroked-button [routerLink]="['/facturas', row.id]" matTooltip="Editar factura y estado de pago (pendiente, parcial, pagada)" class="action-edit">
                <mat-icon>edit</mat-icon>
                Editar
              </a>
              }
              @if (auth.canMutate() && mostrarRecordatorioManual(row)) {
                <button
                  mat-icon-button
                  color="primary"
                  (click)="enviarRecordatorioCliente(row)"
                  [disabled]="loadingRecordatorioId === row.id"
                  matTooltip="Enviar recordatorio de cobro al cliente (email)"
                >
                  @if (loadingRecordatorioId === row.id) {
                    <mat-progress-spinner diameter="22" mode="indeterminate" />
                  } @else {
                    <mat-icon>schedule_send</mat-icon>
                  }
                </button>
              }
              <button mat-icon-button (click)="enviarEmail(row)" matTooltip="Enviar por email">
                <mat-icon>email</mat-icon>
              </button>
              <button mat-icon-button (click)="downloadPdf(row)" matTooltip="Descargar PDF">
                <mat-icon>picture_as_pdf</mat-icon>
              </button>
              @if (auth.canMutate()) {
              <button mat-icon-button color="warn" (click)="delete(row)" matTooltip="Eliminar">
                <mat-icon>delete</mat-icon>
              </button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell" colspan="7">No hay facturas que coincidan con el filtro</td>
          </tr>
        </table>
        <mat-paginator [pageSizeOptions]="[10, 25, 50]" showFirstLastButtons></mat-paginator>
        </div>
      </div>
      }
    </div>
  `,
  styles: [`
    .factura-list {
      max-width: 1400px;
      margin: 0 auto;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--app-space-lg, 24px);
    }

    .header h1 {
      font-size: 1.5rem;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
      margin: 0;
    }

    .header-actions {
      display: flex;
      gap: var(--app-space-sm, 8px);
      align-items: center;
    }

    .filters-bar {
      display: flex;
      gap: var(--app-space-md, 16px);
      margin-bottom: var(--app-space-md, 16px);
      flex-wrap: wrap;
    }

    .filter-text {
      flex: 1;
      min-width: 200px;
    }

    .filter-estado {
      min-width: 160px;
    }

    .invoice-card {
      background: var(--app-bg-card);
      border-radius: var(--app-radius-lg, 16px);
      box-shadow: var(--app-shadow-md);
      border: 1px solid var(--app-border);
      overflow: hidden;
      transition: all var(--app-transition);
    }

    .table-container {
      overflow-x: auto;
    }

    tr.mat-row {
      transition: background var(--app-transition);
    }

    tr.mat-row:hover .mat-mdc-cell {
      background: rgba(30, 58, 138, 0.03);
    }

    .full-width {
      width: 100%;
    }

    .text-right {
      text-align: right;
    }

    .actions-cell {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
    }

    .action-edit {
      white-space: nowrap;
    }

    .text-muted {
      color: rgba(0, 0, 0, 0.38);
    }

    .estado-chip {
      display: inline-block;
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 0.78rem;
      font-weight: 500;
    }

    .estado-pagada { background: #e8f5e9; color: #2e7d32; }
    .estado-no-pagada { background: #fff3e0; color: #e65100; }
    .estado-parcial { background: #fff8e1; color: #f9a825; }

    .venc-ok { color: rgba(0,0,0,0.6); }
    .venc-warn { color: #e65100; font-weight: 500; }
    .venc-overdue { color: #c62828; font-weight: 500; }
  `],
})
export class FacturaListComponent implements OnInit {
  displayedColumns = ['numeroFactura', 'clienteNombre', 'fechaCreacion', 'fechaVencimiento', 'estadoPago', 'total', 'actions'];
  dataSource = new MatTableDataSource<Factura>([]);
  isLoading = false;
  loadingRecordatorioId: number | null = null;

  /**
   * La tabla está en @if (!isLoading): en ngAfterViewInit aún no existe MatSort/MatPaginator.
   * Setters enlazan el dataSource cuando el DOM ya tiene la tabla.
   */
  @ViewChild(MatSort)
  set matSort(sort: MatSort | undefined) {
    this._sort = sort;
    if (sort) {
      this.dataSource.sort = sort;
    }
  }
  private _sort?: MatSort;

  @ViewChild(MatPaginator)
  set matPaginator(p: MatPaginator | undefined) {
    this._paginator = p;
    if (p) {
      this.dataSource.paginator = p;
    }
  }
  private _paginator?: MatPaginator;

  private textFilter = '';
  private estadoFilter = '';

  constructor(
    public auth: AuthService,
    private facturaService: FacturaService,
    private presupuestoService: PresupuestoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.dataSource.filterPredicate = (data: Factura, filter: string) => {
      const [text, estado] = filter.split('||');
      const textMatch = !text || (
        data.numeroFactura?.toLowerCase().includes(text) ||
        data.clienteNombre?.toLowerCase().includes(text) ||
        data.notas?.toLowerCase().includes(text)
      );
      const estadoMatch = !estado || data.estadoPago === estado;
      return !!(textMatch && estadoMatch);
    };
    this.dataSource.sortingDataAccessor = (row: Factura, column: string): string | number => {
      switch (column) {
        case 'fechaCreacion':
          return row.fechaCreacion ? new Date(row.fechaCreacion).getTime() : 0;
        case 'fechaVencimiento':
          return row.fechaVencimiento ? new Date(row.fechaVencimiento).getTime() : Number.MAX_SAFE_INTEGER;
        case 'total':
          return row.total ?? 0;
        case 'numeroFactura':
        case 'clienteNombre':
        case 'estadoPago':
          return (row[column as keyof Factura] as string) ?? '';
        default:
          return '';
      }
    };
  }

  private connectSortPaginator(): void {
    if (this._sort) {
      this.dataSource.sort = this._sort;
    }
    if (this._paginator) {
      this.dataSource.paginator = this._paginator;
    }
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.facturaService.getAll().subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.isLoading = false;
        queueMicrotask(() => this.connectSortPaginator());
      },
      error: () => {
        this.isLoading = false;
      },
    });
  }

  applyTextFilter(event: Event): void {
    this.textFilter = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.updateFilter();
  }

  applyEstadoFilter(estado: string): void {
    this.estadoFilter = estado;
    this.updateFilter();
  }

  private updateFilter(): void {
    this.dataSource.filter = `${this.textFilter}||${this.estadoFilter}`;
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  getVencimientoClass(factura: Factura): string {
    if (!factura.fechaVencimiento || factura.estadoPago === 'Pagada') return 'venc-ok';
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const venc = new Date(factura.fechaVencimiento);
    venc.setHours(0, 0, 0, 0);
    const diff = Math.ceil((venc.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
    if (diff < 0) return 'venc-overdue';
    if (diff <= 7) return 'venc-warn';
    return 'venc-ok';
  }

  /** Vencimiento en ≤15 días (o ya vencida), con importe pendiente y email de cliente. */
  mostrarRecordatorioManual(f: Factura): boolean {
    if (f.estadoPago === 'Pagada') return false;
    if (!f.fechaVencimiento) return false;
    if (!this.tieneImportePendiente(f)) return false;
    if (!f.clienteEmail?.trim()) return false;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const venc = new Date(f.fechaVencimiento);
    venc.setHours(0, 0, 0, 0);
    const diasHastaVenc = Math.round((venc.getTime() - hoy.getTime()) / (1000 * 60 * 60 * 24));
    return diasHastaVenc <= 15;
  }

  private tieneImportePendiente(f: Factura): boolean {
    if (f.estadoPago === 'Pagada') return false;
    if (f.estadoPago === 'Parcial') {
      const cobrado = f.montoCobrado ?? 0;
      return f.total - cobrado > 0.009;
    }
    return true;
  }

  enviarRecordatorioCliente(factura: Factura): void {
    if (this.loadingRecordatorioId === factura.id) return;
    this.loadingRecordatorioId = factura.id;
    this.facturaService.enviarRecordatorioCliente(factura.id).subscribe({
      next: () => {
        this.loadingRecordatorioId = null;
        this.snackBar.open('Recordatorio enviado al cliente', 'Cerrar', { duration: 4000 });
      },
      error: (err) => {
        this.loadingRecordatorioId = null;
        const msg =
          err.error?.message || err.error?.detail || err.error?.error || 'No se pudo enviar el recordatorio';
        const needsConfig =
          typeof msg === 'string' &&
          (msg.includes('correo') || msg.includes('SMTP') || msg.includes('Configure') || msg.includes('configur'));
        this.snackBar.open(typeof msg === 'string' ? msg : 'Error al enviar', needsConfig ? 'Configurar' : 'Cerrar', {
          duration: needsConfig ? 8000 : 5000,
        }).onAction().subscribe(() => {
          if (needsConfig) {
            this.dialog.open(ConfigEmpresaDialogComponent, { width: '500px', data: { context: 'mail' } });
          }
        });
      },
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
