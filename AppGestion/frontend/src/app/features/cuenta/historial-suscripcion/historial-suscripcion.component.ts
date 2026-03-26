import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { AuthService } from '../../../core/auth/auth.service';
import { SubscriptionService } from '../../../core/services/subscription.service';
import { SubscriptionInvoice } from '../../../core/models/subscription-invoice.model';

@Component({
    selector: 'app-historial-suscripcion',
    imports: [
        CommonModule,
        RouterLink,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatDividerModule,
        MatTableModule,
    ],
    templateUrl: './historial-suscripcion.component.html',
    styleUrl: './historial-suscripcion.component.scss'
})
export class HistorialSuscripcionComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly subscriptionApi = inject(SubscriptionService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly invoices = signal<SubscriptionInvoice[]>([]);
  readonly openingPortal = signal(false);

  readonly displayedColumns = ['fecha', 'numero', 'estado', 'importe', 'acciones'] as const;

  readonly tieneClienteStripe = computed(() => this.auth.user()?.billingPortalAvailable === true);

  readonly estadoVacio = computed(() => {
    if (this.loading() || this.loadError()) return null;
    if (!this.tieneClienteStripe()) return 'sin-cliente';
    if (this.invoices().length === 0) return 'sin-facturas';
    return 'ok';
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.subscriptionApi.getSubscriptionInvoices(36).subscribe({
      next: (list) => {
        this.invoices.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
        this.snackBar.open('No se pudo cargar el historial. Inténtalo de nuevo.', 'Cerrar', { duration: 5000 });
      },
    });
  }

  abrirPortal(): void {
    this.openingPortal.set(true);
    this.subscriptionApi.createPortalSession().subscribe({
      next: (res) => {
        if (res.portalUrl) window.location.href = res.portalUrl;
        else this.openingPortal.set(false);
      },
      error: (err) => {
        this.openingPortal.set(false);
        this.snackBar.open(err.error?.error || 'No se pudo abrir el portal', 'Cerrar', { duration: 4000 });
      },
    });
  }

  fechaFmt(unix: number): string {
    if (!unix) return '—';
    const d = new Date(unix * 1000);
    return d.toLocaleDateString('es-ES', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  estadoEtiqueta(status: string): string {
    const s = (status || '').toLowerCase();
    const map: Record<string, string> = {
      paid: 'Pagada',
      open: 'Pendiente de pago',
      draft: 'Borrador',
      void: 'Anulada',
      uncollectible: 'Incobrable',
    };
    return map[s] ?? status;
  }

  estadoClase(status: string): string {
    const s = (status || '').toLowerCase();
    if (s === 'paid') return 'pill pill--ok';
    if (s === 'open') return 'pill pill--warn';
    if (s === 'void' || s === 'uncollectible') return 'pill pill--bad';
    return 'pill';
  }

  importeMostrado(row: SubscriptionInvoice): string {
    const cur = (row.currency || 'eur').toUpperCase();
    const sym = cur === 'EUR' ? '€' : cur + ' ';
    const s = (row.status || '').toLowerCase();
    const cents = s === 'paid' ? row.amountPaidCents : row.amountDueCents;
    const n = (cents ?? 0) / 100;
    return `${n.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${sym}`.trim();
  }
}
