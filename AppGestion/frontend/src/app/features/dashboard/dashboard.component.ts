import { AfterViewInit, ChangeDetectorRef, Component, DestroyRef, NgZone, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { PresupuestoService } from '../../core/services/presupuesto.service';
import { FacturaService } from '../../core/services/factura.service';
import { MaterialService } from '../../core/services/material.service';
import { GastoService } from '../../core/services/gasto.service';
import { AuthService } from '../../core/auth/auth.service';
import { Presupuesto } from '../../core/models/presupuesto.model';
import { Factura } from '../../core/models/factura.model';
import { Material } from '../../core/models/material.model';
import { Gasto } from '../../core/models/gasto.model';
import { EstadoBadgeComponent } from '../../shared/estado-badge/estado-badge.component';
import { FacturaParcialImporteDialogComponent } from '../../shared/factura-parcial-importe-dialog/factura-parcial-importe-dialog.component';
import { catchError, finalize, of } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

interface PresupuestoStats {
  pendientes: number;
  aceptados: number;
  enEjecucion: number;
  rechazados: number;
  totalValor: number;
}

interface FacturaStats {
  pagadas: number;
  noPagadas: number;
  parciales: number;
  totalFacturado: number;
  totalCobrado: number;
  totalPendiente: number;
}

interface SaludCobros {
  vencidas: number;
  importeVencido: number;
  proximasAVencer: number;
  importeProximas: number;
  ratioCobro: number;
}

interface GastoTrimestreStats {
  year: number;
  trimestre: number;
  count: number;
  totalBase: number;
  totalIva: number;
  totalConIva: number;
}

/** Ingresos por mes para el gráfico (nombre del mes/año y total facturado). */
export interface IngresoPorMes {
  name: string;
  value: number;
  /** Clave YYYY-MM para detalle / interacción. */
  key?: string;
}

/** Cliente ranqueado por número de operaciones (presupuestos + facturas). */
export interface TopCliente {
  clienteId: number;
  clienteNombre: string;
  count: number;
}

@Component({
    selector: 'app-dashboard',
    imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatProgressBarModule,
        MatTooltipModule,
        MatButtonToggleModule,
        MatFormFieldModule,
        MatSelectModule,
        MatDialogModule,
        MatSnackBarModule,
        RouterLink,
        EstadoBadgeComponent,
        TranslateModule,
    ],
    templateUrl: './dashboard.component.html',
    styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, AfterViewInit {
  cargandoPresupuestos = true;
  cargandoFacturas = true;
  cargandoMateriales = true;
  cargandoGastos = true;
  presupuestosCount = 0;
  facturasCount = 0;
  presupuestoStats: PresupuestoStats = {
    pendientes: 0,
    aceptados: 0,
    enEjecucion: 0,
    rechazados: 0,
    totalValor: 0,
  };
  facturaStats: FacturaStats = {
    pagadas: 0,
    noPagadas: 0,
    parciales: 0,
    totalFacturado: 0,
    totalCobrado: 0,
    totalPendiente: 0,
  };
  saludCobros: SaludCobros = {
    vencidas: 0,
    importeVencido: 0,
    proximasAVencer: 0,
    importeProximas: 0,
    ratioCobro: 0,
  };
  recentPresupuestos: Presupuesto[] = [];
  recentFacturas: Factura[] = [];
  recentGastos: Gasto[] = [];
  gastoTrimestreStats: GastoTrimestreStats = {
    year: new Date().getFullYear(),
    trimestre: 1,
    count: 0,
    totalBase: 0,
    totalIva: 0,
    totalConIva: 0,
  };
  /** Datos del gráfico según modo (último mes natural o 12 meses del año elegido). */
  ingresosPorMes: IngresoPorMes[] = [];
  /** Vista: un mes natural anterior o año completo. */
  chartIngresosMode: 'ultimoMes' | 'anio' = 'anio';
  /** Año del gráfico en modo «Año». */
  selectedChartYear = new Date().getFullYear();
  hoveredChartMonth: number | null = null;
  monthDrill: { label: string; value: number; facturasCount: number; key: string } | null = null;
  /** Top clientes por frecuencia (presupuestos + facturas). */
  topClientes: TopCliente[] = [];
  /** Top materiales más utilizados (desde API). */
  topMateriales: Material[] = [];
  private allPresupuestos: Presupuesto[] = [];
  private allFacturas: Factura[] = [];

  actualizandoPresupuestoId: number | null = null;
  actualizandoFacturaId: number | null = null;

  readonly auth = inject(AuthService);

  private readonly estadosPresupuestoCatalogo = ['Pendiente', 'Aceptado', 'Rechazado', 'En ejecución'];
  private readonly estadosPagoFacturaCatalogo = ['No Pagada', 'Parcial', 'Pagada'];

  /** Origen X del gráfico (espacio para eje Y y etiquetas). */
  private readonly chartPadLeft = 70;
  private readonly chartPadRight = 42;
  private readonly chartTopY = 20;
  private readonly chartBottomY = 180;

  /** Ancho útil para el área (viewBox 620). */
  get chartWidth(): number {
    return 620 - this.chartPadLeft - this.chartPadRight;
  }

  get chartStepX(): number {
    const n = this.ingresosPorMes.length;
    if (this.chartIngresosMode === 'ultimoMes') {
      return 0;
    }
    return n <= 1 ? this.chartWidth : this.chartWidth / (n - 1);
  }

  get chartTotal(): number {
    return this.ingresosPorMes.reduce((s, d) => s + d.value, 0);
  }

  get monthDrillInvoiceLine(): string {
    if (!this.monthDrill) return '';
    const c = this.monthDrill.facturasCount;
    const key = c === 1 ? 'dashboard.drillInvoiceCount1' : 'dashboard.drillInvoiceCountN';
    return this.translate.instant(key, { count: c });
  }

  /** Abreviaturas de mes (i18n) para ejes del gráfico. */
  private readonly monthShortKeys = [
    'jan',
    'feb',
    'mar',
    'apr',
    'may',
    'jun',
    'jul',
    'aug',
    'sep',
    'oct',
    'nov',
    'dec',
  ] as const;

  private monthShort(monthIndex0: number): string {
    const key = this.monthShortKeys[monthIndex0];
    return key ? this.translate.instant(`dashboard.month.${key}`) : '';
  }

  private refreshMonthDrillLabel(): void {
    if (!this.monthDrill?.key) return;
    const parts = this.monthDrill.key.split('-').map(Number);
    const y = parts[0];
    const mm = parts[1];
    if (y === undefined || mm === undefined || Number.isNaN(y) || Number.isNaN(mm)) return;
    const label = `${this.monthShort(mm - 1)} ${y}`;
    this.monthDrill = { ...this.monthDrill, label };
  }

  get chartYearOptions(): number[] {
    const y = new Date().getFullYear();
    return [y - 4, y - 3, y - 2, y - 1, y, y + 1];
  }

  get monthDrillMonthIndex(): number | null {
    if (!this.monthDrill?.key) return null;
    const idx = this.ingresosPorMes.findIndex((d) => d.key === this.monthDrill!.key);
    return idx >= 0 ? idx : null;
  }

  get ultimoMesBarRect(): { x: number; y: number; width: number; height: number } | null {
    if (this.chartIngresosMode !== 'ultimoMes' || this.ingresosPorMes.length !== 1) return null;
    const d = this.ingresosPorMes[0];
    const maxVal = this.chartMaxValue;
    const rangeY = this.chartBottomY - this.chartTopY;
    const h = (d.value / maxVal) * rangeY;
    const barW = 140;
    const cx = this.chartPadLeft + this.chartWidth / 2;
    return {
      x: cx - barW / 2,
      y: this.chartBottomY - h,
      width: barW,
      height: h,
    };
  }

  /** Valor máximo del eje Y para la escala. */
  get chartMaxValue(): number {
    const vals = this.ingresosPorMes.map((d) => d.value);
    const m = vals.length === 0 ? 0 : Math.max(...vals);
    return m <= 0 ? 1 : m;
  }

  /** Marcas del eje Y: valor, posición y etiqueta formateada. */
  get chartYTicks(): { value: number; y: number; label: string }[] {
    const maxVal = this.chartMaxValue;
    const rangeY = this.chartBottomY - this.chartTopY;
    const steps = 4;
    const ticks: { value: number; y: number; label: string }[] = [];
    for (let i = 0; i <= steps; i++) {
      const value = (maxVal * i) / steps;
      const y = this.chartBottomY - (value / maxVal) * rangeY;
      const label = this.formatChartTick(value);
      ticks.push({ value, y, label });
    }
    return ticks;
  }

  private formatChartTick(value: number): string {
    if (value >= 1000000) return (value / 1000000).toFixed(1) + 'M';
    if (value >= 1000) return (value / 1000).toFixed(0) + 'k';
    return value.toFixed(0);
  }

  /** Puntos del gráfico anual en coordenadas del SVG. */
  private get ingresosChartPoints(): { x: number; y: number }[] {
    if (this.chartIngresosMode !== 'anio') return [];
    const data = this.ingresosPorMes;
    if (data.length === 0) return [];
    const maxVal = this.chartMaxValue;
    const rangeY = this.chartBottomY - this.chartTopY;
    const stepX = this.chartStepX;
    return data.map((d, i) => ({
      x: this.chartPadLeft + i * stepX,
      y: this.chartBottomY - (d.value / maxVal) * rangeY,
    }));
  }

  /**
   * Curva monótona cúbica (Fritsch–Carlson): pasa por cada mes y redondea
   * los cambios de pendiente sin inventar picos por encima o por debajo de los datos.
   */
  private smoothCurvePath(points: { x: number; y: number }[]): string {
    if (points.length === 0) return '';
    if (points.length === 1) return `M ${this.fmtChart(points[0].x)},${this.fmtChart(points[0].y)}`;
    if (points.length === 2) {
      return `M ${this.fmtChart(points[0].x)},${this.fmtChart(points[0].y)} L ${this.fmtChart(points[1].x)},${this.fmtChart(points[1].y)}`;
    }

    const n = points.length;
    const dx: number[] = [];
    const slope: number[] = [];
    for (let i = 0; i < n - 1; i++) {
      const dxi = points[i + 1].x - points[i].x;
      dx.push(dxi);
      slope.push(dxi === 0 ? 0 : (points[i + 1].y - points[i].y) / dxi);
    }

    const tangent = new Array<number>(n);
    tangent[0] = slope[0];
    tangent[n - 1] = slope[n - 2];
    for (let i = 1; i < n - 1; i++) {
      if (slope[i - 1] * slope[i] <= 0) {
        tangent[i] = 0;
      } else {
        tangent[i] = (slope[i - 1] + slope[i]) / 2;
      }
    }

    for (let i = 0; i < n - 1; i++) {
      if (slope[i] === 0) {
        tangent[i] = 0;
        tangent[i + 1] = 0;
        continue;
      }
      const a = tangent[i] / slope[i];
      const b = tangent[i + 1] / slope[i];
      const h = Math.hypot(a, b);
      if (h > 3) {
        const scale = 3 / h;
        tangent[i] = scale * a * slope[i];
        tangent[i + 1] = scale * b * slope[i];
      }
    }

    let d = `M ${this.fmtChart(points[0].x)},${this.fmtChart(points[0].y)}`;
    for (let i = 0; i < n - 1; i++) {
      const p0 = points[i];
      const p1 = points[i + 1];
      const handle = dx[i] / 3;
      const cp1x = p0.x + handle;
      const cp1y = p0.y + tangent[i] * handle;
      const cp2x = p1.x - handle;
      const cp2y = p1.y - tangent[i + 1] * handle;
      d += ` C ${this.fmtChart(cp1x)},${this.fmtChart(cp1y)} ${this.fmtChart(cp2x)},${this.fmtChart(cp2y)} ${this.fmtChart(p1.x)},${this.fmtChart(p1.y)}`;
    }
    return d;
  }

  private fmtChart(n: number): string {
    if (!Number.isFinite(n)) return '0';
    return (Math.round(n * 100) / 100).toString();
  }

  /** Trazo de la curva de ingresos (solo modo año). */
  get ingresosLinePath(): string {
    return this.smoothCurvePath(this.ingresosChartPoints);
  }

  /** Área bajo la curva de ingresos (solo modo año). */
  get ingresosAreaPath(): string {
    const points = this.ingresosChartPoints;
    const line = this.smoothCurvePath(points);
    if (!line || points.length === 0) return '';
    const last = points[points.length - 1];
    const first = points[0];
    return `${line} L ${this.fmtChart(last.x)},${this.chartBottomY} L ${this.fmtChart(first.x)},${this.chartBottomY} Z`;
  }

  constructor(
    private presupuestoService: PresupuestoService,
    private facturaService: FacturaService,
    private materialService: MaterialService,
    private gastoService: GastoService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
    private translate: TranslateService,
    private destroyRef: DestroyRef,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.translate.onLangChange.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.allFacturas.length) {
        this.ingresosPorMes = this.computeIngresosChartData(this.allFacturas);
      }
      this.refreshMonthDrillLabel();
      this.cdr.detectChanges();
    });
    this.presupuestoService.getAll().pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError(() => this.emptyAfterLoadFail<Presupuesto>('snack.estimatesLoadFail')),
      finalize(() => {
        this.cargandoPresupuestos = false;
        this.scheduleLayoutRefresh();
      }),
    ).subscribe((presupuestos) => {
      this.allPresupuestos = presupuestos;
      this.presupuestosCount = presupuestos.length;
      this.recentPresupuestos = [...presupuestos]
        .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
        .slice(0, 5);
      this.computePresupuestoStats(presupuestos);
      this.computeTopClientes();
    });

    this.facturaService.getAll().pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError(() => this.emptyAfterLoadFail<Factura>('snack.invoiceLoadFail')),
      finalize(() => {
        this.cargandoFacturas = false;
        this.scheduleLayoutRefresh();
      }),
    ).subscribe((facturas) => {
      this.allFacturas = facturas;
      this.facturasCount = facturas.length;
      this.recentFacturas = [...facturas]
        .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
        .slice(0, 5);
      this.computeFacturaStats(facturas);
      this.ingresosPorMes = this.computeIngresosChartData(facturas);
      this.computeTopClientes();
    });

    this.materialService.getTopUsados().pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError(() => this.emptyAfterLoadFail<Material>('snack.materialsLoadFail')),
      finalize(() => {
        this.cargandoMateriales = false;
      }),
    ).subscribe((materiales) => {
      this.topMateriales = materiales;
    });

    this.gastoService.getAll().pipe(
      takeUntilDestroyed(this.destroyRef),
      catchError(() => this.emptyAfterLoadFail<Gasto>('gastos.loadFail')),
      finalize(() => {
        this.cargandoGastos = false;
      }),
    ).subscribe((gastos) => {
      this.recentGastos = [...gastos]
        .sort((a, b) => this.parseLocalDate(b.fecha).getTime() - this.parseLocalDate(a.fecha).getTime())
        .slice(0, 5);
      this.gastoTrimestreStats = this.computeGastoTrimestreStats(gastos);
    });
  }

  gastoTotal(g: Gasto): number {
    return (g.baseImponible ?? 0) + (g.cuotaIva ?? 0);
  }

  private computeGastoTrimestreStats(gastos: Gasto[]): GastoTrimestreStats {
    const now = new Date();
    const year = now.getFullYear();
    const trimestre = Math.floor(now.getMonth() / 3) + 1;
    const startMonth = (trimestre - 1) * 3;
    const start = new Date(year, startMonth, 1);
    const end = new Date(year, startMonth + 3, 0);

    let count = 0;
    let totalBase = 0;
    let totalIva = 0;
    for (const g of gastos) {
      if (!g.fecha) continue;
      const d = this.parseLocalDate(g.fecha);
      if (d < start || d > end) continue;
      count++;
      totalBase += g.baseImponible ?? 0;
      totalIva += g.cuotaIva ?? 0;
    }
    return {
      year,
      trimestre,
      count,
      totalBase,
      totalIva,
      totalConIva: totalBase + totalIva,
    };
  }

  private parseLocalDate(isoDate: string): Date {
    const [y, m, d] = isoDate.split('T')[0].split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  ngAfterViewInit(): void {
    this.scheduleLayoutRefresh();
  }

  private emptyAfterLoadFail<T>(key: string) {
    this.snackBar.open(this.translate.instant(key), this.translate.instant('common.close'), { duration: 5000 });
    return of<T[]>([]);
  }

  /**
   * Tras login el sidenav y las fuentes de iconos pueden no estar listas en el primer paint;
   * MatProgressBar y barras flex a veces quedan con ancho 0 hasta un resize.
   */
  private scheduleLayoutRefresh(): void {
    this.ngZone.runOutsideAngular(() => {
      const bump = () =>
        this.ngZone.run(() => {
          window.dispatchEvent(new Event('resize'));
          this.cdr.markForCheck();
        });
      const afterPaint = () => requestAnimationFrame(() => requestAnimationFrame(bump));
      if (typeof document !== 'undefined' && document.fonts?.ready) {
        void document.fonts.ready.then(afterPaint);
      } else {
        afterPaint();
      }
    });
  }

  private computeTopClientes(): void {
    const byCliente = new Map<number, { nombre: string; count: number }>();
    for (const p of this.allPresupuestos) {
      const prev = byCliente.get(p.clienteId);
      const nombre = p.clienteNombre ?? 'Sin nombre';
      if (prev) prev.count++; else byCliente.set(p.clienteId, { nombre, count: 1 });
    }
    for (const f of this.allFacturas) {
      const prev = byCliente.get(f.clienteId);
      const nombre = f.clienteNombre ?? 'Sin nombre';
      if (prev) prev.count++; else byCliente.set(f.clienteId, { nombre, count: 1 });
    }
    this.topClientes = Array.from(byCliente.entries())
      .map(([clienteId, { nombre, count }]) => ({ clienteId, clienteNombre: nombre, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5);
  }

  private computePresupuestoStats(presupuestos: Presupuesto[]): void {
    let pendientes = 0;
    let aceptados = 0;
    let enEjecucion = 0;
    let rechazados = 0;
    let totalValor = 0;
    for (const p of presupuestos) {
      totalValor += p.total ?? 0;
      const raw = (p.estado ?? '').toLowerCase();
      const e = raw.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
      if (e === 'pendiente') pendientes++;
      else if (e === 'aceptado') aceptados++;
      else if (e === 'rechazado') rechazados++;
      else if (e.includes('ejecucion')) enEjecucion++;
      else pendientes++;
    }
    this.presupuestoStats = { pendientes, aceptados, enEjecucion, rechazados, totalValor };
  }

  private computeFacturaStats(facturas: Factura[]): void {
    let pagadas = 0;
    let noPagadas = 0;
    let parciales = 0;
    let totalFacturado = 0;
    let totalCobrado = 0;
    for (const f of facturas) {
      totalFacturado += f.total ?? 0;
      const e = (f.estadoPago ?? '').toLowerCase();
      if (e === 'pagada') {
        pagadas++;
        totalCobrado += f.total ?? 0;
      } else if (e === 'parcial') {
        parciales++;
        totalCobrado += f.montoCobrado ?? (f.total ?? 0) * 0.5;
      }
      // else: no cobrado — noPagadas no suma a totalCobrado
      if (e !== 'pagada' && e !== 'parcial') noPagadas++;
    }
    this.facturaStats = {
      pagadas,
      noPagadas,
      parciales,
      totalFacturado,
      totalCobrado,
      totalPendiente: totalFacturado - totalCobrado,
    };
    this.computeSaludCobros(facturas, totalFacturado, totalCobrado);
  }

  private computeSaludCobros(facturas: Factura[], totalFacturado: number, totalCobrado: number): void {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const enSieteDias = new Date(hoy);
    enSieteDias.setDate(hoy.getDate() + 7);

    let vencidas = 0;
    let importeVencido = 0;
    let proximasAVencer = 0;
    let importeProximas = 0;

    for (const f of facturas) {
      const e = (f.estadoPago ?? '').toLowerCase();
      if (e === 'pagada') continue;
      if (!f.fechaVencimiento) continue;
      const venc = new Date(f.fechaVencimiento);
      venc.setHours(0, 0, 0, 0);
      if (venc < hoy) {
        vencidas++;
        importeVencido += f.total ?? 0;
      } else if (venc <= enSieteDias) {
        proximasAVencer++;
        importeProximas += f.total ?? 0;
      }
    }

    const ratioCobro = totalFacturado > 0 ? (totalCobrado / totalFacturado) * 100 : 0;

    this.saludCobros = {
      vencidas,
      importeVencido,
      proximasAVencer,
      importeProximas,
      ratioCobro: Math.min(ratioCobro, 100),
    };
  }

  chartLabelX(i: number): number {
    if (this.chartIngresosMode === 'ultimoMes') {
      return this.chartPadLeft + this.chartWidth / 2;
    }
    return this.chartPadLeft + i * this.chartStepX;
  }

  chartHitRectX(i: number): number {
    const w = this.chartHitRectWidth();
    const cx = this.chartPadLeft + i * this.chartStepX;
    let left = Math.max(this.chartPadLeft, cx - w / 2);
    const maxRight = 620 - this.chartPadRight;
    if (left + w > maxRight) {
      left = maxRight - w;
    }
    return left;
  }

  chartHitRectWidth(): number {
    const n = this.ingresosPorMes.length;
    if (n <= 1) return this.chartWidth;
    return Math.min(this.chartWidth / n, this.chartStepX * 1.15);
  }

  chartMonthTooltip(i: number): string {
    const item = this.ingresosPorMes[i];
    if (!item?.key) return '';
    const n = this.countFacturasInMonthKey(item.key);
    const invoiceWord = this.translate.instant(n === 1 ? 'dashboard.invoiceSingular' : 'dashboard.invoicePlural');
    return this.translate.instant('dashboard.chartMonthTooltip', {
      month: item.name,
      year: String(this.selectedChartYear),
      amount: item.value.toFixed(2),
      count: n,
      invoiceWord,
    });
  }

  onChartIngresosModeChange(mode: 'ultimoMes' | 'anio'): void {
    this.chartIngresosMode = mode;
    this.monthDrill = null;
    this.hoveredChartMonth = null;
    this.ingresosPorMes = this.computeIngresosChartData(this.allFacturas);
  }

  onChartYearChange(year: number): void {
    this.selectedChartYear = year;
    this.monthDrill = null;
    this.hoveredChartMonth = null;
    this.ingresosPorMes = this.computeIngresosChartData(this.allFacturas);
  }

  onChartMonthHover(i: number): void {
    this.hoveredChartMonth = i;
  }

  onChartMonthLeave(): void {
    this.hoveredChartMonth = null;
  }

  onChartMonthClick(ev: MouseEvent, i: number): void {
    ev.preventDefault();
    ev.stopPropagation();
    this.selectMonthDetail(i);
  }

  selectMonthDetail(i: number): void {
    if (this.chartIngresosMode !== 'anio') return;
    const item = this.ingresosPorMes[i];
    if (!item?.key) return;
    const [y, m] = item.key.split('-').map(Number);
    const count = this.countFacturasInMonth(y, m - 1);
    this.monthDrill = {
      label: `${this.monthShort(m - 1)} ${y}`,
      value: item.value,
      facturasCount: count,
      key: item.key,
    };
  }

  clearMonthDrill(): void {
    this.monthDrill = null;
  }

  countFacturasInMonthKey(key?: string): number {
    if (!key) return 0;
    const [y, mm] = key.split('-').map(Number);
    return this.countFacturasInMonth(y, mm - 1);
  }

  private countFacturasInMonth(year: number, monthIndex0: number): number {
    let n = 0;
    for (const f of this.allFacturas) {
      const d = new Date(f.fechaCreacion);
      if (d.getFullYear() === year && d.getMonth() === monthIndex0) n++;
    }
    return n;
  }

  private computeIngresosChartData(facturas: Factura[]): IngresoPorMes[] {
    if (this.chartIngresosMode === 'ultimoMes') {
      return this.computeUltimoMesNatural(facturas);
    }
    return this.computeIngresosYear(facturas, this.selectedChartYear);
  }

  private computeUltimoMesNatural(facturas: Factura[]): IngresoPorMes[] {
    const now = new Date();
    const target = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const y = target.getFullYear();
    const m = target.getMonth();
    const key = `${y}-${String(m + 1).padStart(2, '0')}`;
    let total = 0;
    for (const f of facturas) {
      const d = new Date(f.fechaCreacion);
      if (d.getFullYear() === y && d.getMonth() === m) {
        total += f.total ?? 0;
      }
    }
    const name = `${this.monthShort(m)} ${y}`;
    return [{ name, value: total, key }];
  }

  private computeIngresosYear(facturas: Factura[], year: number): IngresoPorMes[] {
    const result: IngresoPorMes[] = [];
    for (let m = 0; m < 12; m++) {
      const key = `${year}-${String(m + 1).padStart(2, '0')}`;
      let total = 0;
      for (const f of facturas) {
        const d = new Date(f.fechaCreacion);
        if (d.getFullYear() === year && d.getMonth() === m) {
          total += f.total ?? 0;
        }
      }
      result.push({ name: this.monthShort(m), value: total, key });
    }
    return result;
  }

  otrosEstadosPresupuesto(actual: string): string[] {
    const a = (actual ?? '').trim();
    return this.estadosPresupuestoCatalogo.filter((e) => !this.equivalentesEstadoEjecucionUi(e, a));
  }

  otrosEstadosPago(actual: string): string[] {
    const n = (x: string) => (x ?? '').trim().toLowerCase();
    const a = n(actual);
    return this.estadosPagoFacturaCatalogo.filter((e) => n(e) !== a);
  }

  cambiarEstadoPresupuesto(p: Presupuesto, nuevo: string): void {
    const actual = (p.estado ?? '').trim();
    if (this.equivalentesEstadoEjecucionUi(nuevo, actual)) return;

    this.actualizandoPresupuestoId = p.id;
    this.presupuestoService.actualizarEstado(p.id, nuevo).subscribe({
      next: (updated) => {
        this.actualizandoPresupuestoId = null;
        this.patchPresupuestoReciente(updated);
        this.snackBar.open(
          this.translate.instant('snack.estimateStatus', { status: updated.estado }),
          this.closeLbl(),
          { duration: 2500 },
        );
      },
      error: (err) => this.snackEstadoError(err, () => (this.actualizandoPresupuestoId = null)),
    });
  }

  cambiarEstadoFactura(f: Factura, nuevo: string): void {
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
    this.actualizandoFacturaId = f.id;
    this.facturaService.actualizarEstadoPago(f.id, nuevo, montoParcial).subscribe({
      next: (updated) => {
        this.actualizandoFacturaId = null;
        this.patchFacturaReciente(updated);
        this.snackBar.open(
          this.translate.instant('snack.paymentStatusUpdated', { status: updated.estadoPago }),
          this.closeLbl(),
          { duration: 2500 },
        );
      },
      error: (err) => this.snackEstadoError(err, () => (this.actualizandoFacturaId = null)),
    });
  }

  private equivalentesEstadoEjecucionUi(a: string, b: string): boolean {
    const ej = ['en ejecución', 'en ejecucion'];
    const la = a.trim().toLowerCase();
    const lb = b.trim().toLowerCase();
    if (ej.includes(la) && ej.includes(lb)) return true;
    return la === lb;
  }

  private patchPresupuestoReciente(updated: Presupuesto): void {
    const patch = (list: Presupuesto[]) => {
      const idx = list.findIndex((x) => x.id === updated.id);
      if (idx >= 0) {
        list[idx] = { ...list[idx], estado: updated.estado };
      }
    };
    patch(this.allPresupuestos);
    patch(this.recentPresupuestos);
    this.recentPresupuestos = [...this.recentPresupuestos];
    this.computePresupuestoStats(this.allPresupuestos);
    this.cdr.markForCheck();
  }

  private patchFacturaReciente(updated: Factura): void {
    const patch = (list: Factura[]) => {
      const idx = list.findIndex((x) => x.id === updated.id);
      if (idx >= 0) {
        list[idx] = {
          ...list[idx],
          estadoPago: updated.estadoPago,
          montoCobrado: updated.montoCobrado,
        };
      }
    };
    patch(this.allFacturas);
    patch(this.recentFacturas);
    this.recentFacturas = [...this.recentFacturas];
    this.computeFacturaStats(this.allFacturas);
    this.ingresosPorMes = this.computeIngresosChartData(this.allFacturas);
    this.cdr.markForCheck();
  }

  private snackEstadoError(err: { error?: { message?: string; detail?: string } }, onDone: () => void): void {
    onDone();
    const msgRaw = err.error?.message ?? err.error?.detail ?? '';
    const fb = this.translate.instant('snack.statusUpdateFail');
    const msg = typeof msgRaw === 'string' && msgRaw.trim() ? msgRaw.trim() : fb;
    this.snackBar.open(msg, this.closeLbl(), { duration: 5000 });
  }

  private closeLbl(): string {
    return this.translate.instant('common.close');
  }
}
