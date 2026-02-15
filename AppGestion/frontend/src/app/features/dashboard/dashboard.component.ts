import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PresupuestoService } from '../../core/services/presupuesto.service';
import { FacturaService } from '../../core/services/factura.service';
import { Presupuesto } from '../../core/models/presupuesto.model';
import { Factura } from '../../core/models/factura.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, RouterLink],
  template: `
    <div class="dashboard">
      <h1>Dashboard</h1>
      <div class="cards-grid">
        <mat-card class="stat-card">
          <mat-card-header>
            <mat-card-title>Presupuestos</mat-card-title>
            <mat-icon class="card-icon">description</mat-icon>
          </mat-card-header>
          <mat-card-content>
            <p class="stat-number">{{ presupuestosCount }}</p>
            <a mat-raised-button color="primary" routerLink="/presupuestos">Ver presupuestos</a>
          </mat-card-content>
        </mat-card>
        <mat-card class="stat-card">
          <mat-card-header>
            <mat-card-title>Facturas</mat-card-title>
            <mat-icon class="card-icon">receipt</mat-icon>
          </mat-card-header>
          <mat-card-content>
            <p class="stat-number">{{ facturasCount }}</p>
            <a mat-raised-button color="primary" routerLink="/facturas">Ver facturas</a>
          </mat-card-content>
        </mat-card>
      </div>
      <div class="recent-section">
        <mat-card>
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
                    <a [routerLink]="['/presupuestos', p.id]">{{ p.clienteNombre }} - {{ p.total | number:'1.2-2' }} €</a>
                  </li>
                }
              </ul>
            }
          </mat-card-content>
        </mat-card>
        <mat-card>
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
                    <a [routerLink]="['/facturas', f.id]">{{ f.numeroFactura }} - {{ f.clienteNombre }} - {{ f.total | number:'1.2-2' }} €</a>
                  </li>
                }
              </ul>
            }
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .dashboard h1 {
      margin-bottom: 24px;
    }

    .cards-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
      margin-bottom: 32px;
    }

    .stat-card mat-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .stat-card .card-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: #3f51b5;
    }

    .stat-number {
      font-size: 2rem;
      font-weight: 500;
      margin: 16px 0;
    }

    .recent-section {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 24px;
    }

    .recent-section mat-card-header {
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
      padding: 8px 0;
      border-bottom: 1px solid #eee;
    }

    .recent-list a {
      text-decoration: none;
      color: #3f51b5;
    }

    .recent-list a:hover {
      text-decoration: underline;
    }

    .empty {
      color: #999;
      font-style: italic;
    }
  `],
})
export class DashboardComponent implements OnInit {
  presupuestosCount = 0;
  facturasCount = 0;
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
    });
    this.facturaService.getAll().subscribe((data) => {
      this.facturasCount = data.length;
      this.recentFacturas = data.slice(0, 5);
    });
  }
}
