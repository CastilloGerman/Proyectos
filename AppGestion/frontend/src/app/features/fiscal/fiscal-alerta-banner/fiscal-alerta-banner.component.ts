import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { FiscalService } from '../../../core/services/fiscal.service';
import { FiscalPlazoActual } from '../../../core/models/fiscal.model';

@Component({
  selector: 'app-fiscal-alerta-banner',
  imports: [MatIconModule],
  templateUrl: './fiscal-alerta-banner.component.html',
  styleUrl: './fiscal-alerta-banner.component.scss',
})
export class FiscalAlertaBannerComponent implements OnInit {
  private readonly fiscal = inject(FiscalService);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = signal<FiscalPlazoActual | null>(null);
  readonly loadError = signal(false);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.fiscal
      .getPlazoActual()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (d) => {
          this.data.set(d);
          this.loading.set(false);
        },
        error: () => {
          this.loadError.set(true);
          this.loading.set(false);
        },
      });
  }

  tituloPlazo(d: FiscalPlazoActual): string {
    return `${d.trimestre} — Plazo: ${this.formatearFecha(d.fechaLimite)}`;
  }

  private formatearFecha(iso: string): string {
    const d = new Date(iso + 'T12:00:00');
    return d.toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  claseEstado(estado: FiscalPlazoActual['estado']): string {
    switch (estado) {
      case 'VERDE':
        return 'fab fab--verde';
      case 'AMARILLO':
        return 'fab fab--amarillo';
      case 'ROJO':
        return 'fab fab--rojo';
      default:
        return 'fab';
    }
  }
}
