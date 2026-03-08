import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PresupuestoService } from '../../core/services/presupuesto.service';
import { FacturaService } from '../../core/services/factura.service';
import { Presupuesto } from '../../core/models/presupuesto.model';
import { Factura } from '../../core/models/factura.model';
import { EstadoBadgeComponent } from '../../shared/estado-badge/estado-badge.component';

interface PresupuestoStats {
  pendientes: number;
  aceptados: number;
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
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
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
        </div>
        <div class="kpi-card kpi-pendiente">
          <div class="kpi-icon">
            <mat-icon>pending_actions</mat-icon>
          </div>
          <div class="kpi-content">
            <span class="kpi-value">{{ facturaStats.totalPendiente | number:'1.2-2' }} €</span>
            <span class="kpi-label">Por cobrar</span>
          </div>
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
                class="bar-segment rechazado"
                [style.flex]="presupuestoStats.rechazados || 0.01"
                [matTooltip]="presupuestoStats.rechazados + ' rechazados'"
              ></div>
            </div>
            <div class="distribution-legend">
              <div class="legend-item">
                <span class="dot pendiente"></span>
                <span>Pendientes: {{ presupuestoStats.pendientes }}</span>
              </div>
              <div class="legend-item">
                <span class="dot aceptado"></span>
                <span>Aceptados: {{ presupuestoStats.aceptados }}</span>
              </div>
              <div class="legend-item">
                <span class="dot rechazado"></span>
                <span>Rechazados: {{ presupuestoStats.rechazados }}</span>
              </div>
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
              <div class="legend-item">
                <span class="dot no-pagada"></span>
                <span>No pagadas: {{ facturaStats.noPagadas }}</span>
              </div>
              <div class="legend-item">
                <span class="dot parcial"></span>
                <span>Parciales: {{ facturaStats.parciales }}</span>
              </div>
              <div class="legend-item">
                <span class="dot pagada"></span>
                <span>Pagadas: {{ facturaStats.pagadas }}</span>
              </div>
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
          <a mat-button routerLink="/facturas" class="salud-cta">Ver facturas</a>
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
          <a mat-button routerLink="/facturas" class="salud-cta">Ver facturas</a>
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
              <p class="chart-subtitle">Suma del total facturado en cada mes según la fecha de emisión de las facturas.</p>
            </div>
          </mat-card-header>
          <mat-card-content>
            @if (ingresosPorMes.length === 0) {
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
                  <text x="324" y="218" class="chart-axis-label" text-anchor="middle">Mes</text>
                  <!-- Área de ingresos (path ajustado al nuevo origen 70) -->
                  <path [attr.d]="ingresosAreaPath" fill="url(#ingresosGradient)" stroke="#1e3a8a" stroke-width="2" stroke-linejoin="round"/>
                  <!-- Etiquetas de meses -->
                  @for (item of ingresosPorMes; track item.name; let i = $index) {
                    <text
                      [attr.x]="70 + (i + 0.5) * chartStepX"
                      y="198"
                      class="chart-label"
                      text-anchor="middle"
                    >{{ item.name }}</text>
                  }
                </svg>
                <div class="chart-legend">
                  <span class="chart-legend-desc">Suma total del periodo mostrado:</span>
                  <span class="chart-legend-total">{{ chartTotal | number:'1.2-2' }} €</span>
                </div>
              </div>
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
    }

    .kpi-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--app-shadow-lg);
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
      color: rgba(0, 0, 0, 0.7);
    }

    .dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
    }

    .dot.pendiente { background: #ffc107; }
    .dot.aceptado { background: #4caf50; }
    .dot.rechazado { background: #f44336; }
    .dot.no-pagada { background: #f44336; }
    .dot.parcial { background: #ff9800; }
    .dot.pagada { background: #4caf50; }

    .stats-total, .payment-summary {
      font-size: 0.9rem;
      color: rgba(0, 0, 0, 0.6);
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

    .recent-card {
      border: 1px solid var(--app-border);
    }

    .recent-card mat-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
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

    .recent-name {
      font-weight: 500;
      color: #1e3a8a;
    }

    .recent-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 0.875rem;
      color: rgba(0, 0, 0, 0.6);
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
      color: rgba(0, 0, 0, 0.5);
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
    }

    .salud-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--app-shadow-lg);
    }

    .salud-vencidas { border-left-color: #e2e8f0; }
    .salud-vencidas.salud-alert { border-left-color: #ef4444; background: #fef2f2; }
    .salud-proximas { border-left-color: #e2e8f0; }
    .salud-proximas.salud-warn { border-left-color: #f59e0b; background: #fffbeb; }
    .salud-ratio { border-left-color: #1e3a8a; }

    .salud-icon mat-icon {
      font-size: 28px;
      width: 28px;
      height: 28px;
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
      color: rgba(0,0,0,0.87);
      line-height: 1.2;
    }

    .salud-label {
      font-size: 0.85rem;
      color: rgba(0,0,0,0.6);
    }

    .salud-importe {
      font-size: 0.875rem;
      font-weight: 500;
      color: rgba(0,0,0,0.75);
    }

    .salud-cta {
      align-self: flex-start;
      margin-top: 4px;
    }

    .salud-bar-wrap {
      margin-top: 4px;
    }
  `],
})
export class DashboardComponent implements OnInit {
  presupuestosCount = 0;
  facturasCount = 0;
  presupuestoStats: PresupuestoStats = {
    pendientes: 0,
    aceptados: 0,
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
  /** Últimos 12 meses: ingresos (total facturado) por mes para el gráfico. */
  ingresosPorMes: IngresoPorMes[] = [];

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
    return n <= 1 ? this.chartWidth : this.chartWidth / (n - 1);
  }

  get chartTotal(): number {
    return this.ingresosPorMes.reduce((s, d) => s + d.value, 0);
  }

  /** Valor máximo del eje Y para la escala. */
  get chartMaxValue(): number {
    const vals = this.ingresosPorMes.map((d) => d.value);
    return vals.length === 0 ? 1 : Math.max(...vals);
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

  /** Path SVG del área de ingresos. */
  get ingresosAreaPath(): string {
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
    private facturaService: FacturaService
  ) {}

  ngOnInit(): void {
    this.presupuestoService.getAll().subscribe((data) => {
      this.presupuestosCount = data.length;
      this.recentPresupuestos = [...data]
        .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
        .slice(0, 5);
      this.computePresupuestoStats(data);
    });
    this.facturaService.getAll().subscribe((data) => {
      this.facturasCount = data.length;
      this.recentFacturas = [...data]
        .sort((a, b) => new Date(b.fechaCreacion).getTime() - new Date(a.fechaCreacion).getTime())
        .slice(0, 5);
      this.computeFacturaStats(data);
      this.ingresosPorMes = this.computeIngresosPorMes(data);
    });
  }

  private computePresupuestoStats(presupuestos: Presupuesto[]): void {
    let pendientes = 0;
    let aceptados = 0;
    let rechazados = 0;
    let totalValor = 0;
    for (const p of presupuestos) {
      totalValor += p.total ?? 0;
      const e = (p.estado ?? '').toLowerCase();
      if (e === 'pendiente') pendientes++;
      else if (e === 'aceptado') aceptados++;
      else if (e === 'rechazado') rechazados++;
      else pendientes++;
    }
    this.presupuestoStats = { pendientes, aceptados, rechazados, totalValor };
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

  private computeIngresosPorMes(facturas: Factura[]): IngresoPorMes[] {
    const byMonth = new Map<string, number>();
    for (const f of facturas) {
      const d = new Date(f.fechaCreacion);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      byMonth.set(key, (byMonth.get(key) ?? 0) + (f.total ?? 0));
    }
    const entries = Array.from(byMonth.entries())
      .sort((a, b) => a[0].localeCompare(b[0]))
      .slice(-12);
    return entries.map(([key, value]) => {
      const [y, m] = key.split('-').map(Number);
      const name = `${DashboardComponent.MESES[m - 1]} ${y}`;
      return { name, value };
    });
  }
}
