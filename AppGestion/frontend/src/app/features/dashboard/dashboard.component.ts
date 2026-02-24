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
                        <span class="estado-badge" [class]="getPresupuestoEstadoClass(p.estado)">{{ p.estado }}</span>
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
                        <span class="pago-badge" [class]="getFacturaPagoClass(f.estadoPago)">{{ f.estadoPago }}</span>
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
      padding: 0 24px 48px;
    }

    .dashboard-header {
      margin-bottom: 32px;
    }

    .dashboard-header h1 {
      margin: 0 0 4px 0;
      font-size: 1.75rem;
      font-weight: 500;
    }

    .subtitle {
      margin: 0;
      color: rgba(0, 0, 0, 0.6);
      font-size: 0.95rem;
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 20px;
      margin-bottom: 32px;
    }

    .kpi-card {
      background: linear-gradient(135deg, var(--kpi-bg) 0%, var(--kpi-bg-end) 100%);
      border-radius: 16px;
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      transition: transform 0.2s, box-shadow 0.2s;
    }

    .kpi-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 28px rgba(0, 0, 0, 0.12);
    }

    .kpi-presupuestos {
      --kpi-bg: #e8eaf6;
      --kpi-bg-end: #c5cae9;
    }

    .kpi-facturas {
      --kpi-bg: #e3f2fd;
      --kpi-bg-end: #bbdefb;
    }

    .kpi-facturado {
      --kpi-bg: #e8f5e9;
      --kpi-bg-end: #c8e6c9;
    }

    .kpi-pendiente {
      --kpi-bg: #fff3e0;
      --kpi-bg-end: #ffe0b2;
    }

    .kpi-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .kpi-presupuestos .kpi-icon mat-icon { color: #3f51b5; }
    .kpi-facturas .kpi-icon mat-icon { color: #1976d2; }
    .kpi-facturado .kpi-icon mat-icon { color: #388e3c; }
    .kpi-pendiente .kpi-icon mat-icon { color: #f57c00; }

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
      color: rgba(0, 0, 0, 0.87);
      line-height: 1.2;
    }

    .kpi-label {
      font-size: 0.875rem;
      color: rgba(0, 0, 0, 0.6);
    }

    .kpi-link {
      align-self: flex-start;
      margin-top: 4px;
    }

    .stats-section {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 24px;
      margin-bottom: 32px;
    }

    .stats-card mat-card-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
    }

    .section-icon {
      width: 40px;
      height: 40px;
      font-size: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .section-icon.presupuesto {
      background: #e8eaf6;
      color: #3f51b5;
    }

    .section-icon.factura {
      background: #e3f2fd;
      color: #1976d2;
    }

    .distribution-bar {
      display: flex;
      height: 12px;
      border-radius: 6px;
      overflow: hidden;
      background: #e0e0e0;
      margin-bottom: 16px;
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

    .recent-section {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
      gap: 24px;
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
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    }

    .recent-list li:last-child {
      border-bottom: none;
    }

    .recent-list a {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      text-decoration: none;
      color: inherit;
      transition: background 0.15s;
    }

    .recent-list a:hover {
      background: rgba(0, 0, 0, 0.04);
      margin: 0 -16px;
      padding: 12px 16px;
    }

    .recent-name {
      font-weight: 500;
      color: #3f51b5;
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
  recentPresupuestos: Presupuesto[] = [];
  recentFacturas: Factura[] = [];

  constructor(
    private presupuestoService: PresupuestoService,
    private facturaService: FacturaService
  ) {}

  ngOnInit(): void {
    this.presupuestoService.getAll().subscribe((data) => {
      this.presupuestosCount = data.length;
      this.recentPresupuestos = data.slice(0, 5);
      this.computePresupuestoStats(data);
    });
    this.facturaService.getAll().subscribe((data) => {
      this.facturasCount = data.length;
      this.recentFacturas = data.slice(0, 5);
      this.computeFacturaStats(data);
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

  getPresupuestoEstadoClass(estado: string | undefined): string {
    return 'estado-' + (estado ?? '').toLowerCase();
  }

  getFacturaPagoClass(estadoPago: string | undefined): string {
    return 'pago-' + (estadoPago ?? '').replace(/\s/g, '-').toLowerCase();
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
        totalCobrado += (f.total ?? 0) * 0.5;
      } else {
        noPagadas++;
      }
    }
    this.facturaStats = {
      pagadas,
      noPagadas,
      parciales,
      totalFacturado,
      totalCobrado,
      totalPendiente: totalFacturado - totalCobrado,
    };
  }
}
