import { Component, DestroyRef, OnInit, ViewChild, AfterViewInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { EstadoBadgeComponent } from '../../../shared/estado-badge/estado-badge.component';
import { AuthService } from '../../../core/auth/auth.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { Presupuesto, PresupuestoItemRequest, PresupuestoRequest } from '../../../core/models/presupuesto.model';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';
import { ConfigEmpresaDialogComponent } from '../../../shared/config-empresa-dialog/config-empresa-dialog.component';
import { EnviarEmailDialogComponent } from '../../../shared/enviar-email-dialog/enviar-email-dialog.component';
import { CompletarClienteFiscalDialogComponent } from '../../../shared/completar-cliente-fiscal-dialog/completar-cliente-fiscal-dialog.component';
import { Observable } from 'rxjs';

@Component({
    selector: 'app-presupuesto-list',
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
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatSortModule,
        MatPaginatorModule,
        EstadoBadgeComponent,
    ],
    template: `
    <div class="presupuesto-list">
      <div class="header">
        <h1>Presupuestos</h1>
        <div class="header-actions">
          <button mat-icon-button (click)="openConfig()" matTooltip="Textos al final del PDF">
            <mat-icon>settings</mat-icon>
          </button>
          @if (auth.canMutate()) {
          <a mat-raised-button color="primary" routerLink="/presupuestos/nuevo">
            <mat-icon>add</mat-icon>
            Nuevo presupuesto
          </a>
          }
        </div>
      </div>
      <div class="filters-bar">
        <mat-form-field appearance="outline" class="filter-text">
          <mat-label>Buscar</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput (input)="applyTextFilter($event)" placeholder="Cliente…">
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-estado">
          <mat-label>Estado</mat-label>
          <mat-select [value]="estadoFilter" (selectionChange)="applyEstadoFilter($event.value)">
            <mat-option value="">Todos</mat-option>
            <mat-option value="Pendiente">Pendiente</mat-option>
            <mat-option value="Aceptado">Aceptado</mat-option>
            <mat-option value="Rechazado">Rechazado</mat-option>
            <mat-option value="En ejecución">En ejecución</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <div class="saas-table-card">
        <div class="table-container">
        <table mat-table [dataSource]="dataSource" matSort class="full-width">
          <ng-container matColumnDef="clienteNombre">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Cliente</th>
            <td mat-cell *matCellDef="let row">{{ row.clienteNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="fechaCreacion">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Fecha</th>
            <td mat-cell *matCellDef="let row">{{ row.fechaCreacion | date:'dd/MM/yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Estado</th>
            <td mat-cell *matCellDef="let row">
              @if (auth.canMutate()) {
                <app-estado-badge
                  [estado]="row.estado"
                  [menuOptions]="otrosEstadosPresupuesto(row.estado)"
                  [menuDisabled]="actualizandoEstadoPresupuestoId === row.id"
                  (estadoSeleccionado)="cambiarEstadoPresupuesto(row, $event)"
                ></app-estado-badge>
              } @else {
                <app-estado-badge [estado]="row.estado"></app-estado-badge>
              }
            </td>
          </ng-container>
          <ng-container matColumnDef="anticipo">
            <th mat-header-cell *matHeaderCellDef>Anticipo</th>
            <td mat-cell *matCellDef="let row" class="anticipo-cell">
              @if (row.tieneAnticipo && row.importeAnticipo != null && row.importeAnticipo > 0) {
                <span [matTooltip]="row.anticipoFacturado ? 'Anticipo facturado' : 'Anticipo registrado (pendiente de factura de anticipo)'">
                  {{ row.importeAnticipo | number:'1.2-2' }} €
                  @if (row.anticipoFacturado) {
                    <mat-icon class="anticipo-ok">receipt</mat-icon>
                  }
                </span>
              } @else {
                <span class="text-muted">—</span>
              }
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
              <a mat-stroked-button [routerLink]="['/presupuestos', row.id]" matTooltip="Editar presupuesto y estado (pendiente, aceptado, rechazado)" class="action-edit">
                <mat-icon>edit</mat-icon>
                Editar
              </a>
              }
              @if (auth.canMutate() && puedeFacturar(row)) {
                <button mat-stroked-button color="accent" (click)="crearFactura(row)" matTooltip="Generar factura en un clic desde este presupuesto" class="action-facturar">
                  <mat-icon>receipt_long</mat-icon>
                  Facturar
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
            <td class="mat-cell" colspan="5">No hay presupuestos que coincidan con el filtro</td>
          </tr>
        </table>
        <mat-paginator [pageSizeOptions]="[10, 25, 50]" showFirstLastButtons></mat-paginator>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .presupuesto-list {
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

    .saas-table-card {
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

    .estado-chip {
      display: inline-block;
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 0.78rem;
      font-weight: 500;
    }

    .estado-pendiente { background: #fff3e0; color: #e65100; }
    .estado-aceptado { background: #e8f5e9; color: #2e7d32; }
    .estado-rechazado { background: #ffebee; color: #c62828; }

    .anticipo-cell { font-size: 0.875rem; }
    .anticipo-ok { font-size: 16px; width: 16px; height: 16px; vertical-align: middle; color: #1565c0; }
    .text-muted { color: rgba(0,0,0,0.38); }
  `]
})
export class PresupuestoListComponent implements OnInit, AfterViewInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly estadosPresupuestoCatalogo = ['Pendiente', 'Aceptado', 'Rechazado', 'En ejecución'];
  displayedColumns = ['clienteNombre', 'fechaCreacion', 'estado', 'anticipo', 'total', 'actions'];
  dataSource = new MatTableDataSource<Presupuesto>([]);
  actualizandoEstadoPresupuestoId: number | null = null;

  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  private textFilter = '';
  estadoFilter = '';

  constructor(
    public auth: AuthService,
    private route: ActivatedRoute,
    private presupuestoService: PresupuestoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
    this.dataSource.filterPredicate = (data: Presupuesto, filter: string) => {
      const [text, estado] = filter.split('||');
      const textMatch = !text || data.clienteNombre?.toLowerCase().includes(text);
      const estadoMatch = !estado || data.estado === estado;
      return !!(textMatch && estadoMatch);
    };
  }

  ngOnInit(): void {
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const e = params['estado'];
      if (e === 'Pendiente' || e === 'Aceptado' || e === 'Rechazado' || e === 'En ejecución') {
        this.estadoFilter = e;
      } else {
        this.estadoFilter = '';
      }
      if (this.dataSource.data.length) {
        this.updateFilter();
      }
    });
    this.load();
  }

  ngAfterViewInit(): void {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
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

  applyTextFilter(event: Event): void {
    this.textFilter = (event.target as HTMLInputElement).value.trim().toLowerCase();
    this.updateFilter();
  }

  applyEstadoFilter(estado: string): void {
    this.estadoFilter = estado;
    this.updateFilter();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { estado: estado || null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private updateFilter(): void {
    this.dataSource.filter = `${this.textFilter}||${this.estadoFilter}`;
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  otrosEstadosPresupuesto(actual: string): string[] {
    const a = (actual ?? '').trim();
    return this.estadosPresupuestoCatalogo.filter((e) => !this.equivalentesEstadoEjecucionUi(e, a));
  }

  /** Misma semántica que la API/UI: variantes con o sin tilde cuentan como el mismo estado. */
  private equivalentesEstadoEjecucionUi(a: string, b: string): boolean {
    const ej = ['en ejecución', 'en ejecucion'];
    const la = a.trim().toLowerCase();
    const lb = b.trim().toLowerCase();
    if (ej.includes(la) && ej.includes(lb)) return true;
    return la === lb;
  }

  cambiarEstadoPresupuesto(p: Presupuesto, nuevo: string): void {
    const actual = (p.estado ?? '').trim();
    if (this.equivalentesEstadoEjecucionUi(nuevo, actual)) return;
    this.actualizandoEstadoPresupuestoId = p.id;
    const payload = this.buildPresupuestoUpdateRequest(p, nuevo);
    this.presupuestoService.update(p.id, payload).subscribe({
      next: (updated) => {
        this.actualizandoEstadoPresupuestoId = null;
        this.patchPresupuestoEnTabla(updated);
        this.snackBar.open(`Estado: ${updated.estado}`, 'Cerrar', { duration: 2500 });
      },
      error: (err) => {
        this.actualizandoEstadoPresupuestoId = null;
        const msg = err.error?.message ?? err.error?.detail ?? 'No se pudo actualizar el estado';
        this.snackBar.open(typeof msg === 'string' ? msg : 'Error al actualizar el estado', 'Cerrar', { duration: 5000 });
      },
    });
  }

  private buildPresupuestoUpdateRequest(p: Presupuesto, estado: string): PresupuestoRequest {
    const items: PresupuestoItemRequest[] = (p.items ?? []).map((it) => {
      const manual = it.esTareaManual === true;
      return {
        materialId: manual ? undefined : it.materialId,
        tareaManual: manual ? (it.descripcion?.trim() || undefined) : undefined,
        cantidad: it.cantidad,
        precioUnitario: it.precioUnitario,
        visiblePdf: it.visiblePdf,
      };
    });
    return {
      clienteId: p.clienteId,
      items,
      ivaHabilitado: p.ivaHabilitado,
      estado,
      descuentoGlobalPorcentaje: p.descuentoGlobalPorcentaje,
      descuentoGlobalFijo: p.descuentoGlobalFijo,
      descuentoAntesIva: p.descuentoAntesIva ?? true,
      condicionesActivas: p.condicionesActivas ?? [],
      notaAdicional: p.notaAdicional ?? undefined,
    };
  }

  private patchPresupuestoEnTabla(updated: Presupuesto): void {
    const rows = [...this.dataSource.data];
    const idx = rows.findIndex((x) => x.id === updated.id);
    if (idx >= 0) {
      rows[idx] = { ...rows[idx], estado: updated.estado };
      this.dataSource.data = rows;
    }
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

  openConfig(): void {
    this.dialog.open(ConfigEmpresaDialogComponent, {
      width: '500px',
      data: { context: 'presupuesto' },
    });
  }

  /** Muestra Facturar si el presupuesto admite conversión y aún no tiene factura principal. */
  puedeFacturar(p: Presupuesto): boolean {
    if (p.facturaId != null && p.facturaId !== undefined) return false;
    const e = (p.estado || '').trim().toLowerCase();
    return e === 'pendiente' || e === 'aceptado' || e === 'en ejecución' || e === 'en ejecucion';
  }

  crearFactura(presupuesto: Presupuesto): void {
    const ejecutarFactura = () => {
      const req = presupuesto.tieneAnticipo
        ? this.presupuestoService.createFacturaFinalFromPresupuesto(presupuesto.id)
        : this.presupuestoService.createFacturaFromPresupuesto(presupuesto.id);
      req.subscribe({
        next: (factura) => {
          const msg = presupuesto.tieneAnticipo
            ? 'Factura final creada (si aún no existía, también se emitió la factura de anticipo).'
            : 'Factura creada. El presupuesto ha pasado a estado Aceptado.';
          this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
          this.load();
          this.router.navigate(['/facturas', factura.id]);
        },
        error: (err) => {
          const msg = err.error?.message ?? err.error?.detail ?? '';
          const texto = typeof msg === 'string' ? msg : '';
          if (
            err.status === 400 &&
            texto.includes('completar los datos fiscales')
          ) {
            this.abrirCompletarCliente(presupuesto).subscribe((ok) => {
              if (ok) ejecutarFactura();
            });
          } else {
            this.snackBar.open(texto || 'Error al crear factura', 'Cerrar', { duration: 5000 });
          }
        },
      });
    };

    if (presupuesto.clienteEstado === 'PROVISIONAL') {
      this.abrirCompletarCliente(presupuesto).subscribe((ok) => {
        if (ok) ejecutarFactura();
      });
    } else {
      ejecutarFactura();
    }
  }

  private abrirCompletarCliente(p: Presupuesto): Observable<boolean> {
    return this.dialog
      .open(CompletarClienteFiscalDialogComponent, {
        width: '520px',
        maxWidth: '95vw',
        data: { clienteId: p.clienteId, clienteNombre: p.clienteNombre },
      })
      .afterClosed();
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
