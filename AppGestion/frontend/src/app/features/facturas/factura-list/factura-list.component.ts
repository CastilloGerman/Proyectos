import { Component, DestroyRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { trigger, transition, query, stagger, animate, style } from '@angular/animations';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { EstadoBadgeComponent } from '../../../shared/estado-badge/estado-badge.component';
import { SkeletonComponent } from '../../../shared/skeleton/skeleton.component';
import { AuthService } from '../../../core/auth/auth.service';
import { FacturaService } from '../../../core/services/factura.service';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { Factura } from '../../../core/models/factura.model';
import { Presupuesto } from '../../../core/models/presupuesto.model';
import { ImportarPresupuestoDialogComponent } from '../../../shared/importar-presupuesto-dialog/importar-presupuesto-dialog.component';
import { ConfigEmpresaDialogComponent } from '../../../shared/config-empresa-dialog/config-empresa-dialog.component';
import { EnviarEmailDialogComponent } from '../../../shared/enviar-email-dialog/enviar-email-dialog.component';
import { AnularFacturaDialogComponent } from '../../../shared/anular-factura-dialog/anular-factura-dialog.component';
import { FacturaParcialImporteDialogComponent } from '../../../shared/factura-parcial-importe-dialog/factura-parcial-importe-dialog.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-factura-list',
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
        MatCheckboxModule,
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
          <mat-select [value]="estadoFilter" (selectionChange)="applyEstadoFilter($event.value)">
            <mat-option value="">Todos</mat-option>
            <mat-option value="No Pagada">No Pagada</mat-option>
            <mat-option value="Parcial">Parcial</mat-option>
            <mat-option value="Pagada">Pagada</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-venc">
          <mat-label>Vencimiento</mat-label>
          <mat-select [value]="vencimientoFilter" (selectionChange)="applyVencimientoFilter($event.value)">
            <mat-option value="">Todos</mat-option>
            <mat-option value="vencidas">Vencidas (no cobradas)</mat-option>
            <mat-option value="proximas7">Próximos 7 días</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-emision-anio" matTooltip="Filtra por fecha de emisión de la factura">
          <mat-label>Año emisión</mat-label>
          <mat-select [value]="emisionYear" (selectionChange)="onEmisionYearChange($event.value)">
            <mat-option value="">Todos</mat-option>
            @for (y of emisionYearOptions; track y) {
              <mat-option [value]="'' + y">{{ y }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="filter-emision-mes" matTooltip="Requiere elegir un año">
          <mat-label>Mes emisión</mat-label>
          <mat-select [value]="emisionMonth" [disabled]="!emisionYear" (selectionChange)="onEmisionMonthChange($event.value)">
            <mat-option value="">Todo el año</mat-option>
            @for (m of mesesEmisionCatalogo; track m.value) {
              <mat-option [value]="m.value">{{ m.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-checkbox
          class="filter-pendiente"
          [checked]="pendienteCobroOnly"
          (change)="applyPendienteCobroFilter($event.checked)"
        >
          Solo pendientes de cobro
        </mat-checkbox>
        <mat-checkbox
          class="filter-anuladas"
          [checked]="incluirAnuladas"
          (change)="applyIncluirAnuladas($event.checked)"
        >
          Mostrar anuladas
        </mat-checkbox>
      </div>

      @if (isLoading) {
        <app-skeleton [rows]="10"></app-skeleton>
      } @else {
      <div class="invoice-card" [@listAnimation]>
        <div class="table-container">
        <table mat-table [dataSource]="dataSource" matSort class="full-width">
          <ng-container matColumnDef="numeroFactura">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Nº Factura</th>
            <td mat-cell *matCellDef="let row">
              @if (row.anulada) {
                <mat-chip class="anulada-chip">Anulada</mat-chip>
              }
              {{ row.numeroFactura }}
            </td>
          </ng-container>
          <ng-container matColumnDef="tipoFactura">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let row">
              @if (row.tipoFactura && row.tipoFactura !== 'NORMAL') {
                <mat-chip class="tipo-chip" [class.tipo-anticipo]="row.tipoFactura === 'ANTICIPO'" [class.tipo-final]="row.tipoFactura === 'FINAL_CON_ANTICIPO'">
                  {{ tipoFacturaLabel(row.tipoFactura) }}
                </mat-chip>
              } @else {
                <span class="text-muted">—</span>
              }
            </td>
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
              @if (auth.canMutate() && !row.anulada) {
                <app-estado-badge
                  [estado]="row.estadoPago"
                  [menuOptions]="otrosEstadosPago(row.estadoPago)"
                  [menuDisabled]="actualizandoEstadoFacturaId === row.id"
                  (estadoSeleccionado)="cambiarEstadoPagoFactura(row, $event)"
                ></app-estado-badge>
              } @else {
                <app-estado-badge [estado]="row.estadoPago"></app-estado-badge>
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
              @if (auth.canMutate() && !row.anulada) {
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
              @if (!row.anulada) {
              <button mat-icon-button (click)="enviarEmail(row)" matTooltip="Enviar por email">
                <mat-icon>email</mat-icon>
              </button>
              }
              <button mat-icon-button (click)="downloadPdf(row)" matTooltip="Descargar PDF">
                <mat-icon>picture_as_pdf</mat-icon>
              </button>
              @if (auth.canMutate() && !row.anulada) {
              <button mat-icon-button color="warn" (click)="anular(row)" matTooltip="Anular factura">
                <mat-icon>cancel</mat-icon>
              </button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          <tr class="mat-row" *matNoDataRow>
            <td class="mat-cell" colspan="8">No hay facturas que coincidan con el filtro</td>
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

    .filter-venc {
      min-width: 200px;
    }

    .filter-emision-anio {
      min-width: 120px;
    }

    .filter-emision-mes {
      min-width: 140px;
    }

    .filter-emision-anio .mat-mdc-form-field-subscript-wrapper,
    .filter-emision-mes .mat-mdc-form-field-subscript-wrapper {
      display: none;
    }

    .filter-pendiente {
      align-self: center;
      margin-left: 4px;
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

    .tipo-chip { font-size: 0.75rem; min-height: 26px; padding: 0 10px; }
    .tipo-chip.tipo-anticipo { background: #e3f2fd; color: #1565c0; }
    .tipo-chip.tipo-final { background: #f3e5f5; color: #6a1b9a; }

    .anulada-chip {
      font-size: 0.72rem;
      min-height: 22px;
      margin-right: 6px;
      vertical-align: middle;
      background: #fce4ec;
      color: #ad1457;
    }
  `]
})
export class FacturaListComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly translate = inject(TranslateService);

  private snackClose(): string {
    return this.translate.instant('common.close');
  }

  private snackConfigure(): string {
    return this.translate.instant('common.configure');
  }

  private readonly estadosPagoFacturaCatalogo = ['No Pagada', 'Parcial', 'Pagada'];
  displayedColumns = ['numeroFactura', 'tipoFactura', 'clienteNombre', 'fechaCreacion', 'fechaVencimiento', 'estadoPago', 'total', 'actions'];
  dataSource = new MatTableDataSource<Factura>([]);
  isLoading = false;
  loadingRecordatorioId: number | null = null;
  actualizandoEstadoFacturaId: number | null = null;

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
  estadoFilter = '';
  vencimientoFilter = '';
  pendienteCobroOnly = false;
  incluirAnuladas = false;
  /** Filtro por fecha de emisión: año (YYYY) y opcionalmente mes (01–12). */
  emisionYear = '';
  emisionMonth = '';

  readonly mesesEmisionCatalogo = [
    { value: '01', label: 'Enero' },
    { value: '02', label: 'Febrero' },
    { value: '03', label: 'Marzo' },
    { value: '04', label: 'Abril' },
    { value: '05', label: 'Mayo' },
    { value: '06', label: 'Junio' },
    { value: '07', label: 'Julio' },
    { value: '08', label: 'Agosto' },
    { value: '09', label: 'Septiembre' },
    { value: '10', label: 'Octubre' },
    { value: '11', label: 'Noviembre' },
    { value: '12', label: 'Diciembre' },
  ];

  tipoFacturaLabel(t: string): string {
    if (t === 'ANTICIPO') return 'Anticipo';
    if (t === 'FINAL_CON_ANTICIPO') return 'Final';
    return t;
  }

  get emisionYearOptions(): number[] {
    const cy = new Date().getFullYear();
    const out: number[] = [];
    for (let y = cy + 1; y >= cy - 15; y--) {
      out.push(y);
    }
    return out;
  }

  constructor(
    public auth: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private facturaService: FacturaService,
    private presupuestoService: PresupuestoService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.dataSource.filterPredicate = (data: Factura, filter: string) => {
      const parts = filter.split('||');
      const text = parts[0] ?? '';
      const estado = parts[1] ?? '';
      const venc = parts[2] ?? '';
      const pend = parts[3] ?? '0';
      const emision = parts[4] ?? '';
      const textMatch = !text || (
        data.numeroFactura?.toLowerCase().includes(text) ||
        data.clienteNombre?.toLowerCase().includes(text) ||
        data.notas?.toLowerCase().includes(text)
      );
      const estadoMatch = !estado || data.estadoPago === estado;
      const vencMatch = FacturaListComponent.matchesVencimientoFilter(data, venc);
      const pendMatch = pend !== '1' || FacturaListComponent.matchesPendienteCobroFilter(data);
      const emisionMatch = FacturaListComponent.matchesEmisionFilter(data, emision);
      return !!(textMatch && estadoMatch && vencMatch && pendMatch && emisionMatch);
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
    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
      const ep = params['estadoPago'];
      if (ep === 'No Pagada' || ep === 'Parcial' || ep === 'Pagada') {
        this.estadoFilter = ep;
      } else {
        this.estadoFilter = '';
      }
      const v = params['vencimiento'];
      if (v === 'vencidas' || v === 'proximas7') {
        this.vencimientoFilter = v;
      } else {
        this.vencimientoFilter = '';
      }
      this.pendienteCobroOnly = params['pendienteCobro'] === '1';
      const em = params['emision'];
      if (em && /^\d{4}-\d{2}$/.test(em)) {
        const [y, mo] = em.split('-');
        this.emisionYear = y;
        this.emisionMonth = mo;
      } else if (em && /^\d{4}$/.test(em)) {
        this.emisionYear = em;
        this.emisionMonth = '';
      } else {
        this.emisionYear = '';
        this.emisionMonth = '';
      }
      if (this.dataSource.data.length) {
        this.updateFilter();
      }
    });
    this.load();
  }

  private static matchesPendienteCobroFilter(f: Factura): boolean {
    return (f.estadoPago ?? '').toLowerCase() !== 'pagada';
  }

  /** Misma lógica que el dashboard (salud de cobros). */
  /** Token vacío, YYYY (todo el año) o YYYY-MM (mes concreto). */
  private static matchesEmisionFilter(f: Factura, emision: string): boolean {
    if (!emision) return true;
    if (!f.fechaCreacion) return false;
    const d = new Date(f.fechaCreacion);
    if (Number.isNaN(d.getTime())) return false;
    if (emision.length === 4) {
      const y = parseInt(emision, 10);
      return d.getFullYear() === y;
    }
    if (emision.length === 7 && emision.includes('-')) {
      const [ys, ms] = emision.split('-');
      const y = parseInt(ys, 10);
      const m = parseInt(ms, 10);
      return d.getFullYear() === y && d.getMonth() + 1 === m;
    }
    return true;
  }

  private static matchesVencimientoFilter(f: Factura, v: string): boolean {
    if (!v) return true;
    const e = (f.estadoPago ?? '').toLowerCase();
    if (e === 'pagada') return false;
    if (!f.fechaVencimiento) return false;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const venc = new Date(f.fechaVencimiento);
    venc.setHours(0, 0, 0, 0);
    const enSieteDias = new Date(hoy);
    enSieteDias.setDate(hoy.getDate() + 7);
    if (v === 'vencidas') return venc < hoy;
    if (v === 'proximas7') return venc >= hoy && venc <= enSieteDias;
    return true;
  }

  applyIncluirAnuladas(checked: boolean): void {
    this.incluirAnuladas = checked;
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.facturaService.getAll(undefined, this.incluirAnuladas).subscribe({
      next: (data) => {
        this.dataSource.data = data;
        this.isLoading = false;
        queueMicrotask(() => {
          this.connectSortPaginator();
          this.updateFilter();
        });
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
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { estadoPago: estado || null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  applyVencimientoFilter(v: string): void {
    this.vencimientoFilter = v;
    this.updateFilter();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { vencimiento: v || null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  applyPendienteCobroFilter(checked: boolean): void {
    this.pendienteCobroOnly = checked;
    this.updateFilter();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pendienteCobro: checked ? '1' : null },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private buildEmisionQueryParam(): string | null {
    if (!this.emisionYear) return null;
    if (!this.emisionMonth) return this.emisionYear;
    return `${this.emisionYear}-${this.emisionMonth}`;
  }

  onEmisionYearChange(year: string): void {
    this.emisionYear = year || '';
    if (!this.emisionYear) {
      this.emisionMonth = '';
    }
    this.updateFilter();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { emision: this.buildEmisionQueryParam() },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  onEmisionMonthChange(month: string): void {
    this.emisionMonth = month || '';
    this.updateFilter();
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { emision: this.buildEmisionQueryParam() },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private updateFilter(): void {
    const pend = this.pendienteCobroOnly ? '1' : '0';
    const emisionToken = this.buildEmisionFilterToken();
    this.dataSource.filter = `${this.textFilter}||${this.estadoFilter}||${this.vencimientoFilter}||${pend}||${emisionToken}`;
    if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
  }

  /** Misma semántica que el query param `emision`. */
  private buildEmisionFilterToken(): string {
    if (!this.emisionYear) return '';
    if (!this.emisionMonth) return this.emisionYear;
    return `${this.emisionYear}-${this.emisionMonth}`;
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
    if (f.anulada) return false;
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
        this.snackBar.open(this.translate.instant('snack.invoiceReminderSent'), this.snackClose(), { duration: 4000 });
      },
      error: (err) => {
        this.loadingRecordatorioId = null;
        const msgRaw =
          err.error?.message || err.error?.detail || err.error?.error || '';
        const fallback = this.translate.instant('snack.invoiceSendReminderFail');
        const msg = typeof msgRaw === 'string' && msgRaw.trim() ? msgRaw.trim() : fallback;
        const needsConfig =
          typeof msg === 'string' &&
          (msg.toLowerCase().includes('correo') ||
            msg.toLowerCase().includes('smtp') ||
            msg.includes('Configure') ||
            msg.toLowerCase().includes('configur'));
        this.snackBar.open(msg, needsConfig ? this.snackConfigure() : this.snackClose(), {
          duration: needsConfig ? 8000 : 5000,
        }).onAction().subscribe(() => {
          if (needsConfig) {
            this.dialog.open(ConfigEmpresaDialogComponent, { width: '500px', data: { context: 'mail' } });
          }
        });
      },
    });
  }

  anular(factura: Factura): void {
    if (factura.anulada) return;
    const ref = this.dialog.open(AnularFacturaDialogComponent, { width: '480px' });
    ref.afterClosed().subscribe((motivo: string | null | undefined) => {
      if (motivo === undefined) {
        return;
      }
      this.facturaService.anular(factura.id, motivo).subscribe({
        next: () => {
          this.snackBar.open(this.translate.instant('snack.invoiceCanceled'), this.snackClose(), { duration: 3000 });
          this.load();
        },
        error: (err) => {
          const msgRaw = err.error?.message || err.error?.detail || err.error?.error || '';
          const fb = this.translate.instant('snack.invoiceCancelFail');
          const msg = typeof msgRaw === 'string' && msgRaw.trim() ? msgRaw.trim() : fb;
          this.snackBar.open(msg, this.snackClose(), { duration: 5000 });
        },
      });
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
            const req = selected.tieneAnticipo
              ? this.presupuestoService.createFacturaFinalFromPresupuesto(selected.id)
              : this.presupuestoService.createFacturaFromPresupuesto(selected.id);
            req.subscribe({
              next: (factura) => {
                this.snackBar.open(this.translate.instant('snack.invoiceFromBudget'), this.snackClose(), { duration: 3000 });
                this.facturaService.getAll().subscribe((data) => {
                  this.dataSource.data = data;
                });
              },
              error: (err) => {
                const m = err.error?.message;
                this.snackBar.open(
                  typeof m === 'string' && m.trim() ? m : this.translate.instant('snack.invoiceCreateFail'),
                  this.snackClose(),
                  { duration: 4000 },
                );
              },
            });
          }
        });
      },
      error: () => this.snackBar.open(this.translate.instant('snack.budgetsLoadFail'), this.snackClose(), { duration: 3000 }),
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
            this.snackBar.open(this.translate.instant('snack.invoiceEmailSent'), this.snackClose(), { duration: 3000 });
          },
          error: (err) => {
            const msgRaw = err.error?.detail ?? err.error?.message ?? '';
            const msg =
              typeof msgRaw === 'string' && msgRaw.trim()
                ? msgRaw.trim()
                : this.translate.instant('snack.invoiceEmailFail');
            const needsConfig =
              msg.includes('Configure') || msg.toLowerCase().includes('correo de envío');
            this.snackBar.open(msg, needsConfig ? this.snackConfigure() : this.snackClose(), {
              duration: needsConfig ? 8000 : 5000,
            })
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
      error: () => this.snackBar.open(this.translate.instant('snack.pdfDownloadFail'), this.snackClose(), { duration: 3000 }),
    });
  }

  otrosEstadosPago(actual: string): string[] {
    const n = (x: string) => (x ?? '').trim().toLowerCase();
    const a = n(actual);
    return this.estadosPagoFacturaCatalogo.filter((e) => n(e) !== a);
  }

  cambiarEstadoPagoFactura(f: Factura, nuevo: string): void {
    if (f.anulada) return;
    const n = (x: string) => (x ?? '').trim().toLowerCase();
    if (n(nuevo) === n(f.estadoPago ?? '')) return;

    if (nuevo === 'Parcial') {
      this.dialog
        .open(FacturaParcialImporteDialogComponent, {
          width: '440px',
          maxWidth: '95vw',
          data: {
            totalFactura: f.total,
            numeroFactura: f.numeroFactura,
            clienteNombre: f.clienteNombre,
            importeSugerido: f.montoCobrado,
          },
        })
        .afterClosed()
        .subscribe((importe: number | undefined) => {
          if (importe == null) return;
          this.ejecutarActualizacionEstadoFactura(f, nuevo, importe);
        });
      return;
    }

    this.ejecutarActualizacionEstadoFactura(f, nuevo);
  }

  private ejecutarActualizacionEstadoFactura(f: Factura, nuevo: string, montoParcial?: number): void {
    this.actualizandoEstadoFacturaId = f.id;
    this.facturaService.updateEstadoPago(f.id, nuevo, montoParcial).subscribe({
      next: (updated) => {
        this.actualizandoEstadoFacturaId = null;
        this.patchFacturaEnTabla(updated);
        this.snackBar.open(
          this.translate.instant('snack.paymentStatusUpdated', { status: updated.estadoPago }),
          this.snackClose(),
          { duration: 2500 },
        );
      },
      error: (err) => {
        this.actualizandoEstadoFacturaId = null;
        const msgRaw = err.error?.message ?? err.error?.detail ?? '';
        const fb = this.translate.instant('snack.statusUpdateFail');
        const msg = typeof msgRaw === 'string' && msgRaw.trim() ? msgRaw.trim() : fb;
        this.snackBar.open(msg, this.snackClose(), { duration: 5000 });
      },
    });
  }

  private patchFacturaEnTabla(updated: Factura): void {
    const rows = [...this.dataSource.data];
    const idx = rows.findIndex((x) => x.id === updated.id);
    if (idx >= 0) {
      rows[idx] = { ...rows[idx], estadoPago: updated.estadoPago, montoCobrado: updated.montoCobrado };
      this.dataSource.data = rows;
    }
  }
}
