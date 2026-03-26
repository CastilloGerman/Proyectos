import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClienteService } from '../../../core/services/cliente.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ClienteFacturaResumen, ClientePanel } from '../../../core/models/cliente-panel.model';
import { EstadoBadgeComponent } from '../../../shared/estado-badge/estado-badge.component';

@Component({
  selector: 'app-cliente-panel',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    EstadoBadgeComponent,
  ],
  template: `
    <div class="cliente-panel">
      @if (loading) {
        <div class="loading-wrap">
          <mat-spinner diameter="40"></mat-spinner>
        </div>
      } @else if (error) {
        <mat-card class="error-card">
          <p>{{ error }}</p>
          <a mat-stroked-button routerLink="/clientes">Volver a clientes</a>
        </mat-card>
      } @else if (panel) {
        <header class="panel-header">
          <div class="title-row">
            <a mat-icon-button routerLink="/clientes" matTooltip="Lista de clientes">
              <mat-icon>arrow_back</mat-icon>
            </a>
            <div class="titles">
              <h1>{{ panel.cliente.nombre }}</h1>
              <p class="subtitle">Estado del cliente: cobros, presupuestos e historial</p>
            </div>
          </div>
          <div class="contact-line">
            @if (panel.cliente.email) {
              <span><mat-icon inline>email</mat-icon> {{ panel.cliente.email }}</span>
            }
            @if (panel.cliente.telefono) {
              <span><mat-icon inline>phone</mat-icon> {{ panel.cliente.telefono }}</span>
            }
          </div>
          <div class="header-actions">
            @if (auth.canMutate()) {
              <a mat-stroked-button [routerLink]="['/clientes', panel.cliente.id]">
                <mat-icon>edit</mat-icon>
                Editar datos
              </a>
            }
            <a mat-stroked-button [routerLink]="['/presupuestos/nuevo']" [queryParams]="{ clienteId: panel.cliente.id }">
              <mat-icon>note_add</mat-icon>
              Nuevo presupuesto
            </a>
            <a mat-raised-button color="primary" [routerLink]="['/facturas/nuevo']" [queryParams]="{ clienteId: panel.cliente.id }">
              <mat-icon>receipt_long</mat-icon>
              Nueva factura
            </a>
          </div>
        </header>

        <section class="kpi-grid">
          <mat-card class="kpi">
            <mat-icon class="kpi-icon warn">pending_actions</mat-icon>
            <div>
              <div class="kpi-value">{{ panel.totalPendienteCobro | number: '1.2-2' }} €</div>
              <div class="kpi-label">Pendiente de cobro</div>
            </div>
          </mat-card>
          <mat-card class="kpi">
            <mat-icon class="kpi-icon">receipt</mat-icon>
            <div>
              <div class="kpi-value">{{ panel.facturasConPendiente }}</div>
              <div class="kpi-label">Facturas con saldo</div>
            </div>
          </mat-card>
          <mat-card class="kpi">
            <mat-icon class="kpi-icon accent">description</mat-icon>
            <div>
              <div class="kpi-value">{{ panel.presupuestosActivos.length }}</div>
              <div class="kpi-label">Presupuestos activos</div>
              <div class="kpi-hint">Todos salvo rechazados</div>
            </div>
          </mat-card>
        </section>

        <mat-card class="section-card">
          <mat-card-header>
            <mat-card-title>Pendiente de cobro</mat-card-title>
            <mat-card-subtitle>Facturas no pagadas del todo (incluye parciales)</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            @if (facturasPendiente.length === 0) {
              <p class="empty-inline">No hay importe pendiente con este cliente.</p>
            } @else {
              <table mat-table [dataSource]="facturasPendiente" class="full-width">
                <ng-container matColumnDef="numeroFactura">
                  <th mat-header-cell *matHeaderCellDef>Factura</th>
                  <td mat-cell *matCellDef="let row">
                    <a [routerLink]="['/facturas', row.id]">{{ row.numeroFactura }}</a>
                  </td>
                </ng-container>
                <ng-container matColumnDef="fechaVencimiento">
                  <th mat-header-cell *matHeaderCellDef>Vencimiento</th>
                  <td mat-cell *matCellDef="let row">{{ row.fechaVencimiento || '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="estadoPago">
                  <th mat-header-cell *matHeaderCellDef>Estado</th>
                  <td mat-cell *matCellDef="let row">
                    <app-estado-badge [estado]="row.estadoPago"></app-estado-badge>
                  </td>
                </ng-container>
                <ng-container matColumnDef="total">
                  <th mat-header-cell *matHeaderCellDef>Total</th>
                  <td mat-cell *matCellDef="let row">{{ row.total | number: '1.2-2' }} €</td>
                </ng-container>
                <ng-container matColumnDef="pendiente">
                  <th mat-header-cell *matHeaderCellDef>Pendiente</th>
                  <td mat-cell *matCellDef="let row" class="cell-pendiente">{{ row.pendiente | number: '1.2-2' }} €</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colsPendiente"></tr>
                <tr mat-row *matRowDef="let row; columns: colsPendiente"></tr>
              </table>
            }
          </mat-card-content>
        </mat-card>

        <mat-card class="section-card">
          <mat-card-header>
            <mat-card-title>Presupuestos activos</mat-card-title>
            <mat-card-subtitle>Pendiente, aceptado o en ejecución (excluye rechazados)</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            @if (panel.presupuestosActivos.length === 0) {
              <p class="empty-inline">No hay presupuestos activos.</p>
            } @else {
              <table mat-table [dataSource]="panel.presupuestosActivos" class="full-width">
                <ng-container matColumnDef="id">
                  <th mat-header-cell *matHeaderCellDef>ID</th>
                  <td mat-cell *matCellDef="let row">
                    <a [routerLink]="['/presupuestos', row.id]">#{{ row.id }}</a>
                  </td>
                </ng-container>
                <ng-container matColumnDef="fechaCreacion">
                  <th mat-header-cell *matHeaderCellDef>Fecha</th>
                  <td mat-cell *matCellDef="let row">{{ row.fechaCreacion | date: 'short' }}</td>
                </ng-container>
                <ng-container matColumnDef="total">
                  <th mat-header-cell *matHeaderCellDef>Total</th>
                  <td mat-cell *matCellDef="let row">{{ row.total | number: '1.2-2' }} €</td>
                </ng-container>
                <ng-container matColumnDef="estado">
                  <th mat-header-cell *matHeaderCellDef>Estado</th>
                  <td mat-cell *matCellDef="let row">
                    <app-estado-badge [estado]="row.estado"></app-estado-badge>
                  </td>
                </ng-container>
                <ng-container matColumnDef="facturaId">
                  <th mat-header-cell *matHeaderCellDef>Factura</th>
                  <td mat-cell *matCellDef="let row">
                    @if (row.facturaId) {
                      <a [routerLink]="['/facturas', row.facturaId]">Ver</a>
                    } @else {
                      —
                    }
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colsPresupuesto"></tr>
                <tr mat-row *matRowDef="let row; columns: colsPresupuesto"></tr>
              </table>
            }
          </mat-card-content>
        </mat-card>

        <mat-card class="section-card">
          <mat-card-header>
            <mat-card-title>Historial</mat-card-title>
            <mat-card-subtitle>Facturas y presupuestos ordenados del más reciente al más antiguo</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            @if (panel.historial.length === 0) {
              <p class="empty-inline">Aún no hay documentos con este cliente.</p>
            } @else {
              <table mat-table [dataSource]="panel.historial" class="full-width historial-table">
                <ng-container matColumnDef="fechaOrden">
                  <th mat-header-cell *matHeaderCellDef>Fecha</th>
                  <td mat-cell *matCellDef="let row">{{ row.fechaOrden | date: 'short' }}</td>
                </ng-container>
                <ng-container matColumnDef="tipo">
                  <th mat-header-cell *matHeaderCellDef>Tipo</th>
                  <td mat-cell *matCellDef="let row">
                    @if (row.tipo === 'FACTURA') {
                      <span class="tipo-badge factura">Factura</span>
                    } @else {
                      <span class="tipo-badge presupuesto">Presupuesto</span>
                    }
                  </td>
                </ng-container>
                <ng-container matColumnDef="etiqueta">
                  <th mat-header-cell *matHeaderCellDef>Documento</th>
                  <td mat-cell *matCellDef="let row">
                    @if (row.tipo === 'FACTURA') {
                      <a [routerLink]="['/facturas', row.id]">{{ row.etiqueta }}</a>
                    } @else {
                      <a [routerLink]="['/presupuestos', row.id]">{{ row.etiqueta }}</a>
                    }
                    @if (row.subetiqueta) {
                      <div class="subetiqueta">{{ row.subetiqueta }}</div>
                    }
                  </td>
                </ng-container>
                <ng-container matColumnDef="importe">
                  <th mat-header-cell *matHeaderCellDef>Importe</th>
                  <td mat-cell *matCellDef="let row">{{ row.importe | number: '1.2-2' }} €</td>
                </ng-container>
                <ng-container matColumnDef="estado">
                  <th mat-header-cell *matHeaderCellDef>Estado</th>
                  <td mat-cell *matCellDef="let row">
                    <app-estado-badge [estado]="row.estado"></app-estado-badge>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colsHistorial"></tr>
                <tr mat-row *matRowDef="let row; columns: colsHistorial"></tr>
              </table>
            }
          </mat-card-content>
        </mat-card>
      }
    </div>
  `,
  styles: [
    `
      .cliente-panel {
        max-width: 1100px;
        margin: 0 auto;
        padding: 0 8px 48px;
      }
      .loading-wrap {
        display: flex;
        justify-content: center;
        padding: 48px;
      }
      .error-card {
        padding: 24px;
        max-width: 480px;
        margin: 24px auto;
      }
      .panel-header {
        margin-bottom: 24px;
      }
      .title-row {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        margin-bottom: 8px;
      }
      .titles h1 {
        margin: 0;
        font-size: 1.75rem;
      }
      .subtitle {
        margin: 4px 0 0;
        color: var(--app-text-secondary, #64748b);
        font-size: 0.9375rem;
      }
      .contact-line {
        display: flex;
        flex-wrap: wrap;
        gap: 16px;
        margin: 12px 0 16px;
        font-size: 0.9rem;
        color: var(--app-text-secondary, #64748b);
      }
      .contact-line mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
        vertical-align: middle;
        margin-right: 4px;
      }
      .header-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        align-items: center;
      }
      .header-actions mat-icon {
        margin-right: 4px;
        font-size: 18px;
        width: 18px;
        height: 18px;
      }
      .kpi-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 16px;
        margin-bottom: 24px;
      }
      .kpi {
        display: flex;
        align-items: center;
        gap: 16px;
        padding: 16px 20px;
      }
      .kpi-icon {
        font-size: 36px;
        width: 36px;
        height: 36px;
        color: var(--app-text-secondary, #64748b);
      }
      .kpi-icon.warn {
        color: #f9a825;
      }
      .kpi-icon.accent {
        color: var(--app-accent-copper, #b87333);
      }
      .kpi-value {
        font-size: 1.35rem;
        font-weight: 600;
        font-family: 'Sora', 'IBM Plex Sans', sans-serif;
      }
      .kpi-label {
        font-size: 0.875rem;
        color: var(--app-text-secondary, #64748b);
      }
      .kpi-hint {
        font-size: 0.75rem;
        color: var(--app-text-secondary, #64748b);
        margin-top: 4px;
      }
      .section-card {
        margin-bottom: 24px;
      }
      .full-width {
        width: 100%;
      }
      .cell-pendiente {
        font-weight: 600;
        color: #c62828;
      }
      .empty-inline {
        margin: 0;
        color: var(--app-text-secondary, #64748b);
      }
      .historial-table .subetiqueta {
        font-size: 0.8rem;
        color: var(--app-text-secondary, #64748b);
        margin-top: 2px;
      }
      .tipo-badge {
        display: inline-block;
        padding: 2px 8px;
        border-radius: 6px;
        font-size: 0.75rem;
        font-weight: 600;
      }
      .tipo-badge.factura {
        background: #e3f2fd;
        color: #1565c0;
      }
      .tipo-badge.presupuesto {
        background: #fff3e0;
        color: #e65100;
      }
    `,
  ],
})
export class ClientePanelComponent implements OnInit {
  panel: ClientePanel | null = null;
  loading = true;
  error: string | null = null;

  colsPendiente = ['numeroFactura', 'fechaVencimiento', 'estadoPago', 'total', 'pendiente'];
  colsPresupuesto = ['id', 'fechaCreacion', 'total', 'estado', 'facturaId'];
  colsHistorial = ['fechaOrden', 'tipo', 'etiqueta', 'importe', 'estado'];

  facturasPendiente: ClienteFacturaResumen[] = [];

  constructor(
    private route: ActivatedRoute,
    private clienteService: ClienteService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'Cliente no indicado';
      this.loading = false;
      return;
    }
    this.clienteService.getPanel(+id).subscribe({
      next: (p) => {
        this.panel = p;
        this.facturasPendiente = p.facturas.filter((f) => f.pendiente > 0.001);
        this.loading = false;
      },
      error: () => {
        this.error = 'No se pudo cargar el panel del cliente.';
        this.loading = false;
      },
    });
  }
}
