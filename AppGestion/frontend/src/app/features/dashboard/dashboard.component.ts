import { AfterViewInit, ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { PresupuestoService } from '../../core/services/presupuesto.service';
import { FacturaService } from '../../core/services/factura.service';
import { MaterialService } from '../../core/services/material.service';
import { Presupuesto } from '../../core/models/presupuesto.model';
import { Factura } from '../../core/models/factura.model';
import { Material } from '../../core/models/material.model';
import { EstadoBadgeComponent } from '../../shared/estado-badge/estado-badge.component';
import { catchError, forkJoin, of } from 'rxjs';

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
        RouterLink,
        EstadoBadgeComponent,
    ],
    template: `
    <div class="dashboard">
      <header class="dashboard-header">
        <h1>Dashboard</h1>
        <p class="subtitle">Resumen de tu actividad</p>
      </header>

      <section class="kpi-grid">
        <div class="kpi-card kpi-presupuestos">
          <div class="kpi-icon">
            <mat-icon>description</mat-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-value">{{ presupuestosCount }}</span>
            <span class="kpi-label">Presupuestos</span>
          </div>
          <a mat-button routerLink="/presupuestos" class="kpi-link">Ver todos</a>
        </div>
        <div class="kpi-card kpi-facturas">
          <div class="kpi-icon">
            <mat-icon>receipt_long</mat-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-value">{{ facturasCount }}</span>
            <span class="kpi-label">Facturas</span>
          </div>
          <a mat-button routerLink="/facturas" class="kpi-link">Ver todas</a>
        </div>
        <div class="kpi-card kpi-facturado">
          <div class="kpi-icon">
            <mat-icon>euro</mat-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-value">{{ facturaStats.totalFacturado | number:'1.2-2' }} €</span>
            <span class="kpi-label">Total facturado</span>
          </div>
          <a mat-button routerLink="/facturas" class="kpi-link">Ver facturas</a>
        </div>
        <div class="kpi-card kpi-pendiente">
          <div class="kpi-icon">
            <mat-icon>pending_actions</mat-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-value">{{ facturaStats.totalPendiente | number:'1.2-2' }} €</span>
            <span class="kpi-label">Por cobrar</span>
          </div>
          <a mat-button routerLink="/facturas" [queryParams]="{ pendienteCobro: '1' }" class="kpi-link">Ver pendientes</a>
        </div>
      </section>

      <section class="stats-section">
        <mat-card class="stats-card">
          <mat-card-header>
            <mat-icon class="section-icon presupuesto">pie_chart</mat-icon>
            <mat-card-title>Presupuestos por estado</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="distribution-bar">
              <div
                class="bar-segment pendiente"
                [style.flex]="presupuestoStats.pendientes || 0.01"
                [matTooltip]="presupuestoStats.pendientes + ' pendientes'"
              ></div>
              <div
                class="bar-segment aceptado"
                [style.flex]="presupuestoStats.aceptados || 0.01"
                [matTooltip]="presupuestoStats.aceptados + ' aceptados'"
              ></div>
              <div
                class="bar-segment ejecucion"
                [style.flex]="presupuestoStats.enEjecucion || 0.01"
                [matTooltip]="presupuestoStats.enEjecucion + ' en ejecución'"
              ></div>
              <div
                class="bar-segment rechazado"
                [style.flex]="presupuestoStats.rechazados || 0.01"
                [matTooltip]="presupuestoStats.rechazados + ' rechazados'"
              ></div>
            </div>
            <div class="distribution-legend presupuesto-legend">
              @if (presupuestoStats.pendientes > 0) {
                <a
                  class="legend-item legend-link"
                  [routerLink]="['/presupuestos']"
                  [queryParams]="{ estado: 'Pendiente' }"
                  matTooltip="Ver presupuestos pendientes"
                >
                  <span class="dot pendiente"></span>
                  <span>Pendientes: {{ presupuestoStats.pendientes }}</span>
                </a>
              } @else {
                <div class="legend-item">
                  <span class="dot pendiente"></span>
                  <span>Pendientes: {{ presupuestoStats.pendientes }}</span>
                </div>
              }
              @if (presupuestoStats.aceptados > 0) {
                <a
                  class="legend-item legend-link"
                  [routerLink]="['/presupuestos']"
                  [queryParams]="{ estado: 'Aceptado' }"
                  matTooltip="Ver presupuestos aceptados"
                >
                  <span class="dot aceptado"></span>
                  <span>Aceptados: {{ presupuestoStats.aceptados }}</span>
                </a>
              } @else {
                <div class="legend-item">
                  <span class="dot aceptado"></span>
                  <span>Aceptados: {{ presupuestoStats.aceptados }}</span>
                </div>
              }
              @if (presupuestoStats.enEjecucion > 0) {
                <a
                  class="legend-item legend-link"
                  [routerLink]="['/presupuestos']"
                  [queryParams]="{ estado: 'En ejecución' }"
                  matTooltip="Ver presupuestos en ejecución"
                >
                  <span class="dot ejecucion"></span>
                  <span>En ejecución: {{ presupuestoStats.enEjecucion }}</span>
                </a>
              } @else {
                <div class="legend-item">
                  <span class="dot ejecucion"></span>
                  <span>En ejecución: {{ presupuestoStats.enEjecucion }}</span>
                </div>
              }
              @if (presupuestoStats.rechazados > 0) {
                <a
                  class="legend-item legend-link"
                  [routerLink]="['/presupuestos']"
                  [queryParams]="{ estado: 'Rechazado' }"
                  matTooltip="Ver presupuestos rechazados"
                >
                  <span class="dot rechazado"></span>
                  <span>Rechazados: {{ presupuestoStats.rechazados }}</span>
                </a>
              } @else {
                <div class="legend-item">
                  <span class="dot rechazado"></span>
                  <span>Rechazados: {{ presupuestoStats.rechazados }}</span>
                </div>
              }
            </div>
            <p class="stats-total">Valor total presupuestos: {{ presupuestoStats.totalValor | number:'1.2-2' }} €</p>
          </mat-card-content>
        </mat-card>

        <mat-card class="stats-card">
          <mat-card-header>
            <mat-icon class="section-icon factura">account_balance_wallet</mat-icon>
            <mat-card-title>Facturas por estado de pago</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="distribution-bar">
              <div
                class="bar-segment no-pagada"
                [style.flex]="facturaStats.noPagadas || 0.01"
                [matTooltip]="facturaStats.noPagadas + ' no pagadas'"
              ></div>
              <div
                class="bar-segment parcial"
                [style.flex]="facturaStats.parciales || 0.01"
                [matTooltip]="facturaStats.parciales + ' parciales'"
              ></div>
              <div
                class="bar-segment pagada"
                [style.flex]="facturaStats.pagadas || 0.01"
                [matTooltip]="facturaStats.pagadas + ' pagadas'"
              ></div>
            </div>
            <div class="distribution-legend">
              @if (facturaStats.noPagadas > 0) {
                <a
                  class="legend-item legend-link"
                  [routerLink]="['/facturas']"
                  [queryParams]="{ estadoPago: 'No Pagada' }"
                  matTooltip="Ver facturas no pagadas en el listado"
                >
                  <span class="dot no-pagada"></span>
                  <span>No pagadas: {{ facturaStats.noPagadas }}</span>
                </a>
              } @else {
                <div class="legend-item">
                  <span class="dot no-pagada"></span>
                  <span>No pagadas: {{ facturaStats.noPagadas }}</span>
                </div>
              }
              @if (facturaStats.parciales > 0) {
                <a
                  class="legend-item legend-link"
                  [routerLink]="['/facturas']"
                  [queryParams]="{ estadoPago: 'Parcial' }"
                  matTooltip="Ver facturas con pago parcial"
                >
                  <span class="dot parcial"></span>
                  <span>Parciales: {{ facturaStats.parciales }}</span>
                </a>
              } @else {
                <div class="legend-item">
                  <span class="dot parcial"></span>
                  <span>Parciales: {{ facturaStats.parciales }}</span>
                </div>
              }
              @if (facturaStats.pagadas > 0) {
                <a
                  class="legend-item legend-link"
                  [routerLink]="['/facturas']"
                  [queryParams]="{ estadoPago: 'Pagada' }"
                  matTooltip="Ver facturas pagadas"
                >
                  <span class="dot pagada"></span>
                  <span>Pagadas: {{ facturaStats.pagadas }}</span>
                </a>
              } @else {
                <div class="legend-item">
                  <span class="dot pagada"></span>
                  <span>Pagadas: {{ facturaStats.pagadas }}</span>
                </div>
              }
            </div>
            <div class="payment-summary">
              <span class="cobrado">Cobrado: {{ facturaStats.totalCobrado | number:'1.2-2' }} €</span>
              <span class="pendiente">Pendiente: {{ facturaStats.totalPendiente | number:'1.2-2' }} €</span>
            </div>
          </mat-card-content>
        </mat-card>
      </section>

      <section class="salud-section">
        <div class="salud-card salud-vencidas" [class.salud-alert]="saludCobros.vencidas > 0">
          <div class="salud-icon">
            <mat-icon>warning</mat-icon>
          </div>
          <div class="salud-content">
            <span class="salud-value">{{ saludCobros.vencidas }}</span>
            <span class="salud-label">Facturas vencidas</span>
            @if (saludCobros.importeVencido > 0) {
              <span class="salud-importe">{{ saludCobros.importeVencido | number:'1.2-2' }} €</span>
            }
          </div>
          <a
            mat-button
            [routerLink]="['/facturas']"
            [queryParams]="saludCobros.vencidas > 0 ? { vencimiento: 'vencidas' } : {}"
            class="salud-cta"
            [matTooltip]="saludCobros.vencidas > 0 ? 'Listado filtrado: vencidas y no cobradas' : 'Ver todas las facturas'"
          >Ver facturas</a>
        </div>

        <div class="salud-card salud-proximas" [class.salud-warn]="saludCobros.proximasAVencer > 0">
          <div class="salud-icon">
            <mat-icon>schedule</mat-icon>
          </div>
          <div class="salud-content">
            <span class="salud-value">{{ saludCobros.proximasAVencer }}</span>
            <span class="salud-label">Vencen en 7 días</span>
            @if (saludCobros.importeProximas > 0) {
              <span class="salud-importe">{{ saludCobros.importeProximas | number:'1.2-2' }} €</span>
            }
          </div>
          <a
            mat-button
            [routerLink]="['/facturas']"
            [queryParams]="saludCobros.proximasAVencer > 0 ? { vencimiento: 'proximas7' } : {}"
            class="salud-cta"
            [matTooltip]="saludCobros.proximasAVencer > 0 ? 'Listado filtrado: vencen en los próximos 7 días' : 'Ver todas las facturas'"
          >Ver facturas</a>
        </div>

        <div class="salud-card salud-ratio">
          <div class="salud-icon">
            <mat-icon>show_chart</mat-icon>
          </div>
          <div class="salud-content">
            <span class="salud-value">{{ saludCobros.ratioCobro | number:'1.0-0' }}%</span>
            <span class="salud-label">Ratio de cobro</span>
          </div>
          <div class="salud-bar-wrap">
            <mat-progress-bar mode="determinate" [value]="saludCobros.ratioCobro" color="primary"></mat-progress-bar>
          </div>
        </div>
      </section>

      <section class="chart-section">
        <mat-card class="chart-card">
          <mat-card-header>
            <mat-icon class="section-icon chart">show_chart</mat-icon>
            <div class="chart-header-text">
              <mat-card-title>Ingresos por mes</mat-card-title>
              <p class="chart-subtitle">{{ chartSubtitle }}</p>
            </div>
          </mat-card-header>
          <mat-card-content>
            <div class="chart-toolbar">
              <mat-button-toggle-group
                [value]="chartIngresosMode"
                (change)="onChartIngresosModeChange($event.value)"
                appearance="standard"
                class="chart-mode-toggle"
              >
                <mat-button-toggle value="ultimoMes">Último mes</mat-button-toggle>
                <mat-button-toggle value="anio">Año</mat-button-toggle>
              </mat-button-toggle-group>
              @if (chartIngresosMode === 'anio') {
                <mat-form-field appearance="outline" class="chart-filter-year">
                  <mat-label>Año</mat-label>
                  <mat-select [value]="selectedChartYear" (selectionChange)="onChartYearChange($event.value)">
                    @for (y of chartYearOptions; track y) {
                      <mat-option [value]="y">{{ y }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
              }
            </div>
            @if (facturasCount === 0) {
              <p class="chart-empty">No hay datos de facturación para mostrar. Crea facturas para ver el gráfico.</p>
            } @else {
              <div class="chart-wrap" [attr.aria-label]="'Gráfico de ingresos por mes'">
                <svg class="area-chart" viewBox="0 0 620 240" preserveAspectRatio="xMidYMid meet">
                  <defs>
                    <linearGradient id="ingresosGradient" x1="0" x2="0" y1="0" y2="1">
                      <stop offset="0%" stop-color="#1e3a8a" stop-opacity="0.35"/>
                      <stop offset="100%" stop-color="#1e3a8a" stop-opacity="0.02"/>
                    </linearGradient>
                  </defs>
                  <!-- Eje Y: línea y etiqueta -->
                  <line x1="70" y1="20" x2="70" y2="180" stroke="var(--app-border)" stroke-width="1"/>
                  <text x="24" y="100" class="chart-axis-label" text-anchor="middle" transform="rotate(-90 24 100)">Importe (€)</text>
                  <!-- Marcas y valores del eje Y -->
                  @for (tick of chartYTicks; track tick.value) {
                    <line [attr.x1]="70" [attr.y1]="tick.y" [attr.x2]="578" [attr.y2]="tick.y" stroke="var(--app-border)" stroke-width="0.5" stroke-dasharray="4 2"/>
                    <text x="62" [attr.y]="tick.y + 4" class="chart-tick-label" text-anchor="end">{{ tick.label }}</text>
                  }
                  <!-- Eje X: línea -->
                  <line x1="70" y1="180" x2="578" y2="180" stroke="var(--app-border)" stroke-width="1"/>
                  <text x="324" y="218" class="chart-axis-label" text-anchor="middle">{{ chartXAxisLabel }}</text>
                  @if (chartIngresosMode === 'ultimoMes' && ultimoMesBarRect) {
                    <rect
                      [attr.x]="ultimoMesBarRect.x"
                      [attr.y]="ultimoMesBarRect.y"
                      [attr.width]="ultimoMesBarRect.width"
                      [attr.height]="ultimoMesBarRect.height"
                      fill="url(#ingresosGradient)"
                      stroke="#1e3a8a"
                      stroke-width="2"
                      rx="4"
                    />
                  } @else if (chartIngresosMode === 'anio') {
                    <path
                      [attr.d]="ingresosAreaPath"
                      fill="url(#ingresosGradient)"
                      stroke="#1e3a8a"
                      stroke-width="2"
                      stroke-linejoin="round"
                      pointer-events="none"
                    />
                  }
                  <!-- Etiquetas de meses (no capturan el ratón: la capa de clic va encima) -->
                  @for (item of ingresosPorMes; track item.key; let i = $index) {
                    <text
                      [attr.x]="chartLabelX(i)"
                      y="198"
                      class="chart-label"
                      text-anchor="middle"
                      pointer-events="none"
                    >{{ item.name }}</text>
                  }
                  <!-- Zona clic por mes encima del gráfico (transparente no recibe clics sin pointer-events) -->
                  @if (chartIngresosMode === 'anio') {
                    @for (item of ingresosPorMes; track item.key; let i = $index) {
                      <rect
                        class="chart-hit-rect"
                        [attr.x]="chartHitRectX(i)"
                        y="20"
                        [attr.width]="chartHitRectWidth()"
                        height="160"
                        fill="rgba(0,0,0,0)"
                        pointer-events="all"
                        [class.chart-hit-active]="hoveredChartMonth === i || monthDrillMonthIndex === i"
                        (mouseenter)="onChartMonthHover(i)"
                        (mouseleave)="onChartMonthLeave()"
                        (click)="onChartMonthClick($event, i)"
                        (keydown.enter)="selectMonthDetail(i)"
                        role="button"
                        [attr.aria-label]="'Ver detalle de ' + item.name + ' ' + selectedChartYear"
                        tabindex="0"
                      >
                        <title>{{ chartMonthTooltip(i) }}</title>
                      </rect>
                    }
                  }
                </svg>
                <div class="chart-legend">
                  <span class="chart-legend-desc">Suma total del periodo mostrado:</span>
                  <span class="chart-legend-total">{{ chartTotal | number:'1.2-2' }} €</span>
                </div>
                @if (monthDrill && chartIngresosMode === 'anio') {
                  <div class="month-drill">
                    <div class="month-drill-head">
                      <span class="month-drill-title">{{ monthDrill.label }}</span>
                      <button mat-button type="button" (click)="clearMonthDrill()" class="month-drill-close">Cerrar</button>
                    </div>
                    <p class="month-drill-amount">
                      Total facturado: <strong>{{ monthDrill.value | number:'1.2-2' }} €</strong>
                    </p>
                    <p class="month-drill-count">{{ monthDrill.facturasCount }} factura(s) en ese mes</p>
                  </div>
                }
              </div>
            }
          </mat-card-content>
        </mat-card>
      </section>

      <section class="recent-section top-lists-section">
        <mat-card class="recent-card">
          <mat-card-header class="top-clientes-card-header">
            <div class="top-clientes-titles">
              <mat-card-title>Top clientes</mat-card-title>
              <mat-card-subtitle>
                Pendiente de cobro, presupuestos e historial por cliente
              </mat-card-subtitle>
            </div>
            <a mat-button routerLink="/clientes">Ver todos</a>
          </mat-card-header>
          <mat-card-content>
            @if (topClientes.length === 0) {
              <p class="empty">No hay datos de clientes en presupuestos ni facturas</p>
            } @else {
              <ul class="recent-list top-list top-clientes-list">
                @for (c of topClientes; track c.clienteId; let i = $index) {
                  <li class="top-cliente-item">
                    <a [routerLink]="['/clientes', c.clienteId, 'panel']" class="top-cliente-link">
                      <span class="top-rank">#{{ i + 1 }}</span>
                      <span class="recent-name">{{ c.clienteNombre }}</span>
                      <span class="recent-meta">{{ c.count }} operaciones</span>
                    </a>
                    <a
                      mat-stroked-button
                      color="primary"
                      [routerLink]="['/clientes', c.clienteId, 'panel']"
                      class="top-cliente-panel-btn"
                    >
                      Ver estado
                    </a>
                  </li>
                }
              </ul>
            }
          </mat-card-content>
        </mat-card>
        <mat-card class="recent-card">
          <mat-card-header>
            <mat-card-title>Top materiales utilizados</mat-card-title>
            <a mat-button routerLink="/materiales">Ver todos</a>
          </mat-card-header>
          <mat-card-content>
            @if (topMateriales.length === 0) {
              <p class="empty">No hay materiales utilizados</p>
            } @else {
              <ul class="recent-list top-list">
                @for (m of topMateriales; track m.id; let i = $index) {
                  <li>
                    <a [routerLink]="['/materiales']">
                      <span class="top-rank">#{{ i + 1 }}</span>
                      <span class="recent-name">{{ m.nombre }}</span>
                      <span class="recent-meta">{{ m.precioUnitario | number:'1.2-2' }} € / {{ m.unidadMedida }}</span>
                    </a>
                  </li>
                }
              </ul>
            }
          </mat-card-content>
        </mat-card>
      </section>

      <section class="recent-section">
        <mat-card class="recent-card">
          <mat-card-header>
            <mat-card-title>Últimos presupuestos</mat-card-title>
            <a mat-button routerLink="/presupuestos">Ver todos</a>
          </mat-card-header>
          <mat-card-content>
            @if (recentPresupuestos.length === 0) {
              <p class="empty">No hay presupuestos</p>
            } @else {
              <ul class="recent-list">
                @for (p of recentPresupuestos; track p.id) {
                  <li>
                    <a [routerLink]="['/presupuestos', p.id]">
                      <span class="recent-name">{{ p.clienteNombre }}</span>
                      <span class="recent-meta">
                        <app-estado-badge [estado]="p.estado"></app-estado-badge>
                        {{ p.total | number:'1.2-2' }} €
                      </span>
                    </a>
                  </li>
                }
              </ul>
            }
          </mat-card-content>
        </mat-card>
        <mat-card class="recent-card">
          <mat-card-header>
            <mat-card-title>Últimas facturas</mat-card-title>
            <a mat-button routerLink="/facturas">Ver todas</a>
          </mat-card-header>
          <mat-card-content>
            @if (recentFacturas.length === 0) {
              <p class="empty">No hay facturas</p>
            } @else {
              <ul class="recent-list">
                @for (f of recentFacturas; track f.id) {
                  <li>
                    <a [routerLink]="['/facturas', f.id]">
                      <span class="recent-name">{{ f.numeroFactura }} · {{ f.clienteNombre }}</span>
                      <span class="recent-meta">
                        <app-estado-badge [estado]="f.estadoPago"></app-estado-badge>
                        {{ f.total | number:'1.2-2' }} €
                      </span>
                    </a>
                  </li>
                }
              </ul>
            }
          </mat-card-content>
        </mat-card>
      </section>
    </div>
  `,
    styles: [`
    .dashboard {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 var(--app-space-lg, 24px) var(--app-space-xl, 32px);
    }

    .dashboard-header {
      margin-bottom: var(--app-space-xl, 32px);
    }

    .dashboard-header h1 {
      margin: 0 0 var(--app-space-xs, 4px) 0;
      font-size: 1.75rem;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
      letter-spacing: -0.02em;
      font-family: 'Sora', 'Space Grotesk', 'IBM Plex Sans', sans-serif;
    }

    .subtitle {
      margin: 0;
      color: var(--app-text-secondary, #64748b);
      font-size: 0.9375rem;
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: var(--app-space-lg, 24px);
      margin-bottom: var(--app-space-xl, 32px);
    }

    .kpi-card {
      background: var(--app-bg-card);
      border-radius: var(--app-radius-lg, 16px);
      padding: var(--app-space-lg, 24px);
      display: flex;
      flex-direction: column;
      gap: var(--app-space-md, 16px);
      box-shadow: var(--app-shadow-md);
      border: 1px solid var(--app-border);
      transition: transform var(--app-transition), box-shadow var(--app-transition);
      animation: kpi-in 0.45s ease-out both;
      min-width: 0;
    }

    .kpi-card:hover {
      transform: translateY(-3px);
      box-shadow: var(--app-shadow-lg);
    }

    .kpi-card:nth-child(1) { animation-delay: 0.05s; }
    .kpi-card:nth-child(2) { animation-delay: 0.1s; }
    .kpi-card:nth-child(3) { animation-delay: 0.15s; }
    .kpi-card:nth-child(4) { animation-delay: 0.2s; }

    @keyframes kpi-in {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .kpi-presupuestos { border-top: 3px solid #6366f1; }
    .kpi-facturas { border-top: 3px solid #0ea5e9; }
    .kpi-facturado { border-top: 3px solid #22c55e; }
    .kpi-pendiente { border-top: 3px solid #f59e0b; }

    .kpi-icon {
      width: 48px;
      height: 48px;
      border-radius: var(--app-radius-md, 12px);
      background: var(--app-bg-page, #f8fafc);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .kpi-presupuestos .kpi-icon mat-icon { color: #6366f1; }
    .kpi-facturas .kpi-icon mat-icon { color: #0ea5e9; }
    .kpi-facturado .kpi-icon mat-icon { color: #22c55e; }
    .kpi-pendiente .kpi-icon mat-icon { color: #f59e0b; }

    .kpi-icon mat-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
      overflow: visible;
    }

    .kpi-content {
      flex: 1;
    }

    .kpi-value {
      display: block;
      font-size: 1.75rem;
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
      line-height: 1.2;
      letter-spacing: -0.02em;
    }

    .kpi-label {
      font-size: 0.875rem;
      color: var(--app-text-secondary, #64748b);
    }

    .kpi-link {
      align-self: flex-start;
      margin-top: var(--app-space-xs, 4px);
    }

    .stats-section {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: var(--app-space-lg, 24px);
      margin-bottom: var(--app-space-xl, 32px);
    }

    .stats-card {
      border: 1px solid var(--app-border);
      min-width: 0;
    }

    .stats-card mat-card-content {
      min-width: 0;
    }

    .stats-card mat-card-header {
      display: flex;
      align-items: center;
      gap: var(--app-space-md, 16px);
      margin-bottom: var(--app-space-md, 16px);
    }

    .section-icon {
      width: 40px;
      height: 40px;
      font-size: 40px;
      border-radius: var(--app-radius-md, 12px);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      overflow: visible;
    }

    .section-icon.presupuesto {
      background: #eef2ff;
      color: #6366f1;
    }

    .section-icon.factura {
      background: #ecfeff;
      color: #0ea5e9;
    }

    .distribution-bar {
      display: flex;
      width: 100%;
      height: 10px;
      border-radius: var(--app-radius-sm, 8px);
      overflow: hidden;
      background: var(--app-bg-page, #f1f5f9);
      margin-bottom: var(--app-space-md, 16px);
    }

    .bar-segment {
      min-width: 4px;
      transition: flex 0.3s ease;
    }

    .bar-segment.pendiente { background: #ffc107; }
    .bar-segment.aceptado { background: #4caf50; }
    .bar-segment.ejecucion { background: #ff9800; }
    .bar-segment.rechazado { background: #f44336; }
    .bar-segment.no-pagada { background: #f44336; }
    .bar-segment.parcial { background: #ff9800; }
    .bar-segment.pagada { background: #4caf50; }

    .distribution-legend {
      display: flex;
      flex-wrap: wrap;
      gap: 16px 24px;
      margin-bottom: 12px;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.875rem;
      color: var(--app-text-secondary);
    }

    .stats-card .distribution-legend .legend-link {
      color: inherit;
      text-decoration: none;
      border-radius: 6px;
      padding: 2px 4px;
      margin: -2px -4px;
      transition: background 0.15s ease, color 0.15s ease;
    }

    .stats-card .distribution-legend .legend-link:hover {
      background: rgba(30, 58, 138, 0.08);
      color: var(--app-primary, #1e3a8a);
      text-decoration: underline;
    }

    .stats-card .distribution-legend .legend-link:focus-visible {
      outline: 2px solid var(--app-primary, #1e3a8a);
      outline-offset: 2px;
    }

    .dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
    }

    .dot.pendiente { background: #ffc107; }
    .dot.aceptado { background: #4caf50; }
    .dot.ejecucion { background: #ff9800; }
    .dot.rechazado { background: #f44336; }
    .dot.no-pagada { background: #f44336; }
    .dot.parcial { background: #ff9800; }
    .dot.pagada { background: #4caf50; }

    .stats-total, .payment-summary {
      font-size: 0.9rem;
      color: var(--app-text-secondary);
      margin: 0;
    }

    .payment-summary {
      display: flex;
      gap: 24px;
    }

    .payment-summary .cobrado { color: #4caf50; font-weight: 500; }
    .payment-summary .pendiente { color: #f57c00; font-weight: 500; }

    .chart-section {
      margin-bottom: var(--app-space-xl, 32px);
    }

    .chart-card {
      border: 1px solid var(--app-border);
    }

    .chart-card mat-card-header {
      display: flex;
      align-items: flex-start;
      gap: var(--app-space-md, 16px);
      margin-bottom: var(--app-space-md, 16px);
    }

    .chart-header-text {
      flex: 1;
      min-width: 0;
    }

    .chart-header-text .mat-mdc-card-title {
      margin-bottom: 4px;
    }

    .chart-subtitle {
      margin: 0;
      font-size: 0.8125rem;
      color: var(--app-text-secondary, #64748b);
      line-height: 1.4;
    }

    .chart-toolbar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: var(--app-space-md, 16px);
      margin-bottom: var(--app-space-md, 16px);
    }

    .chart-mode-toggle {
      flex-shrink: 0;
    }

    .chart-filter-year {
      width: 120px;
      margin: 0;
    }

    .chart-filter-year .mat-mdc-form-field-subscript-wrapper {
      display: none;
    }

    .chart-hit-rect {
      cursor: pointer;
      outline: none;
    }

    .chart-hit-rect:focus-visible {
      outline: 2px solid var(--app-primary, #1e3a8a);
      outline-offset: -2px;
    }

    .chart-hit-active {
      fill: rgba(30, 58, 138, 0.06);
    }

    .month-drill {
      margin-top: var(--app-space-md, 16px);
      padding: var(--app-space-md, 16px);
      border-radius: var(--app-radius-md, 12px);
      background: var(--app-bg-page, #f8fafc);
      border: 1px solid var(--app-border);
    }

    .month-drill-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .month-drill-title {
      font-weight: 600;
      font-size: 1rem;
      color: var(--app-text-primary, #0f172a);
    }

    .month-drill-close {
      flex-shrink: 0;
    }

    .month-drill-amount,
    .month-drill-count {
      margin: 4px 0 0 0;
      font-size: 0.875rem;
      color: var(--app-text-secondary, #64748b);
    }

    .section-icon.chart {
      width: 40px;
      height: 40px;
      font-size: 40px;
      border-radius: var(--app-radius-md, 12px);
      background: #eef2ff;
      color: #1e3a8a;
    }

    .chart-wrap {
      min-height: 280px;
    }

    .area-chart {
      width: 100%;
      max-width: 100%;
      height: auto;
      display: block;
    }

    .chart-axis-label {
      font-size: 11px;
      fill: var(--app-text-secondary, #64748b);
      font-weight: 500;
    }

    .chart-tick-label {
      font-size: 10px;
      fill: var(--app-text-muted, #94a3b8);
    }

    .chart-label {
      font-size: 11px;
      fill: var(--app-text-secondary, #64748b);
    }

    .chart-legend {
      margin-top: var(--app-space-md, 16px);
      padding-top: var(--app-space-sm, 8px);
      border-top: 1px solid var(--app-border);
      font-size: 0.875rem;
      display: flex;
      align-items: baseline;
      gap: 8px;
      flex-wrap: wrap;
    }

    .chart-legend-desc {
      color: var(--app-text-secondary, #64748b);
    }

    .chart-legend-total {
      font-weight: 600;
      color: var(--app-text-primary, #0f172a);
    }

    .chart-empty {
      margin: 0;
      padding: var(--app-space-xl, 32px);
      text-align: center;
      color: var(--app-text-muted, #94a3b8);
      font-size: 0.9375rem;
    }

    .recent-section {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
      gap: var(--app-space-lg, 24px);
    }

    .top-lists-section {
      margin-bottom: var(--app-space-xl, 32px);
    }

    .recent-card {
      border: 1px solid var(--app-border);
    }

    .recent-card mat-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .top-clientes-card-header {
      align-items: flex-start;
    }

    .top-clientes-titles {
      flex: 1;
      min-width: 0;
    }

    .top-clientes-titles mat-card-subtitle {
      margin-top: 4px;
      line-height: 1.35;
    }

    .top-clientes-list .top-cliente-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 4px 0;
    }

    .top-cliente-link {
      flex: 1;
      min-width: 0;
    }

    .top-cliente-panel-btn {
      flex-shrink: 0;
      font-size: 0.8125rem;
    }

    .recent-list {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .recent-list li {
      border-bottom: 1px solid var(--app-border);
    }

    .recent-list li:last-child {
      border-bottom: none;
    }

    .recent-list a {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--app-space-md, 16px) 0;
      text-decoration: none;
      color: inherit;
      transition: background var(--app-transition);
      border-radius: var(--app-radius-md, 12px);
    }

    .recent-list a:hover {
      background: var(--app-bg-page, #f8fafc);
    }

    .top-cliente-link:hover {
      background: var(--app-bg-page, #f8fafc);
    }

    .recent-name {
      font-weight: 600;
      color: var(--app-text-primary);
    }

    .recent-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 0.875rem;
      color: var(--app-text-secondary);
    }

    .top-list .top-rank {
      font-weight: 600;
      color: var(--app-text-secondary, #64748b);
      min-width: 28px;
      font-size: 0.8125rem;
    }

    .estado-badge, .pago-badge {
      font-size: 0.7rem;
      padding: 2px 8px;
      border-radius: 12px;
      font-weight: 500;
    }

    .estado-pendiente, .pago-no-pagada { background: #fff3e0; color: #e65100; }
    .estado-aceptado, .pago-pagada { background: #e8f5e9; color: #2e7d32; }
    .estado-rechazado { background: #ffebee; color: #c62828; }
    .pago-parcial { background: #fff8e1; color: #f9a825; }

    .empty {
      color: var(--app-text-muted);
      font-style: italic;
      padding: 24px 0;
    }

    .salud-section {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: var(--app-space-lg, 24px);
      margin-bottom: var(--app-space-xl, 32px);
    }

    .salud-card {
      background: var(--app-bg-card);
      border-radius: var(--app-radius-lg, 16px);
      padding: var(--app-space-lg, 24px);
      display: flex;
      flex-direction: column;
      gap: var(--app-space-sm, 8px);
      box-shadow: var(--app-shadow-md);
      border: 1px solid var(--app-border);
      border-left: 4px solid var(--app-border);
      transition: transform var(--app-transition), box-shadow var(--app-transition);
      min-width: 0;
    }

    .salud-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--app-shadow-lg);
    }

    .salud-vencidas { border-left-color: #e2e8f0; }
    .salud-vencidas.salud-alert { border-left-color: #ef4444; background: rgba(239, 68, 68, 0.1); }
    .salud-proximas { border-left-color: #e2e8f0; }
    .salud-proximas.salud-warn { border-left-color: #f59e0b; background: rgba(245, 158, 11, 0.12); }
    .salud-ratio { border-left-color: #1e3a8a; }

    .salud-icon mat-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
      overflow: visible;
    }

    .salud-vencidas.salud-alert .salud-icon mat-icon { color: #ef4444; }
    .salud-proximas.salud-warn .salud-icon mat-icon { color: #f59e0b; }
    .salud-ratio .salud-icon mat-icon { color: #1e3a8a; }
    .salud-icon mat-icon { color: #94a3b8; }

    .salud-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .salud-value {
      font-size: 1.75rem;
      font-weight: 600;
      color: var(--app-text-primary);
      line-height: 1.2;
    }

    .salud-label {
      font-size: 0.85rem;
      color: var(--app-text-secondary);
    }

    .salud-importe {
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--app-text-secondary);
    }

    .salud-cta {
      align-self: flex-start;
      margin-top: 4px;
    }

    .salud-bar-wrap {
      margin-top: 4px;
      width: 100%;
    }

    :host-context(html.app-dark-theme) .salud-vencidas:not(.salud-alert),
    :host-context(html.app-dark-theme) .salud-proximas:not(.salud-warn) {
      border-left-color: rgba(255, 255, 255, 0.14);
    }

    :host-context(html.app-dark-theme) .salud-proximas.salud-warn {
      background: rgba(180, 83, 9, 0.22);
      border-left-color: #fbbf24;
    }

    :host-context(html.app-dark-theme) .salud-vencidas.salud-alert {
      background: rgba(127, 29, 29, 0.35);
      border-left-color: #f87171;
    }

    :host-context(html.app-dark-theme) .section-icon.presupuesto {
      background: rgba(99, 102, 241, 0.22);
      color: #c7d2fe;
    }

    :host-context(html.app-dark-theme) .section-icon.factura {
      background: rgba(14, 165, 233, 0.18);
      color: #7dd3fc;
    }

    :host-context(html.app-dark-theme) .section-icon.chart {
      background: rgba(99, 102, 241, 0.2);
      color: #a5b4fc;
    }

    :host-context(html.app-dark-theme) .stats-card .distribution-legend .legend-link:hover {
      background: rgba(129, 140, 248, 0.14);
      color: #e0e7ff;
    }

    :host-context(html.app-dark-theme) .chart-hit-active {
      fill: rgba(129, 140, 248, 0.2);
    }

    :host-context(html.app-dark-theme) .kpi-icon {
      background: rgba(255, 255, 255, 0.06);
    }
  `]
})
export class DashboardComponent implements OnInit, AfterViewInit {
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

  get chartSubtitle(): string {
    if (this.chartIngresosMode === 'ultimoMes') {
      return 'Total facturado en el último mes natural (mes anterior al actual), según fecha de emisión.';
    }
    return 'Enero a diciembre del año seleccionado. Pasa el cursor sobre un mes o haz clic para ver importe y número de facturas.';
  }

  get chartXAxisLabel(): string {
    return this.chartIngresosMode === 'ultimoMes' ? 'Periodo' : 'Mes';
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

  /** Path SVG del área de ingresos (solo modo año). */
  get ingresosAreaPath(): string {
    if (this.chartIngresosMode !== 'anio') return '';
    const data = this.ingresosPorMes;
    if (data.length === 0) return '';
    const maxVal = this.chartMaxValue;
    const rangeY = this.chartBottomY - this.chartTopY;
    const stepX = this.chartStepX;
    const points: string[] = [];
    data.forEach((d, i) => {
      const x = this.chartPadLeft + i * stepX;
      const y = this.chartBottomY - (d.value / maxVal) * rangeY;
      points.push(`${x},${y}`);
    });
    const first = points[0];
    const lastX = this.chartPadLeft + (data.length <= 1 ? 0 : (data.length - 1) * stepX);
    return `M ${first} L ${points.slice(1).join(' L ')} L ${lastX},${this.chartBottomY} L ${this.chartPadLeft},${this.chartBottomY} Z`;
  }

  constructor(
    private presupuestoService: PresupuestoService,
    private facturaService: FacturaService,
    private materialService: MaterialService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    forkJoin({
      presupuestos: this.presupuestoService.getAll().pipe(catchError(() => of<Presupuesto[]>([]))),
      facturas: this.facturaService.getAll().pipe(catchError(() => of<Factura[]>([]))),
      materiales: this.materialService.getTopUsados().pipe(catchError(() => of<Material[]>([]))),
    }).subscribe({
      next: ({ presupuestos, facturas, materiales }) => {
        this.allPresupuestos = presupuestos;
        this.presupuestosCount = presupuestos.length;
        this.recentPresupuestos = [...presupuestos]
          .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
          .slice(0, 5);
        this.computePresupuestoStats(presupuestos);

        this.allFacturas = facturas;
        this.facturasCount = facturas.length;
        this.recentFacturas = [...facturas]
          .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
          .slice(0, 5);
        this.computeFacturaStats(facturas);
        this.ingresosPorMes = this.computeIngresosChartData(facturas);

        this.topMateriales = materiales;
        this.computeTopClientes();
        this.scheduleLayoutRefresh();
      },
    });
  }

  ngAfterViewInit(): void {
    this.scheduleLayoutRefresh();
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

  private static readonly MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

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
    return `${item.name} ${this.selectedChartYear}: ${item.value.toFixed(2)} € (${n} factura${n === 1 ? '' : 's'})`;
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
      label: `${DashboardComponent.MESES[m - 1]} ${y}`,
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
    const name = `${DashboardComponent.MESES[m]} ${y}`;
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
      result.push({ name: DashboardComponent.MESES[m], value: total, key });
    }
    return result;
  }
}
