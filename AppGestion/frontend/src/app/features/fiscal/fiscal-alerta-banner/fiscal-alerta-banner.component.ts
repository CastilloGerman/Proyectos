import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { FiscalService } from '../../../core/services/fiscal.service';
import { FiscalPlazoActual } from '../../../core/models/fiscal.model';
import {
  TrimestreId,
  etiquetaPlazo,
  ocultarPlazo,
  parseEtiquetaPlazo,
  plazoEnRojo,
  plazoEstaOculto,
} from '../declaraciones-hacienda/hacienda-periodo';

@Component({
  selector: 'app-fiscal-alerta-banner',
  imports: [MatIconModule],
  templateUrl: './fiscal-alerta-banner.component.html',
  styleUrl: './fiscal-alerta-banner.component.scss',
})
export class FiscalAlertaBannerComponent implements OnInit {
  private readonly fiscal = inject(FiscalService);
  private readonly destroyRef = inject(DestroyRef);

  /** null hasta saber si ese trimestre tiene facturas o gastos. */
  @Input() tieneMovimientos: boolean | null = null;
  @Output() plazoCargado = new EventEmitter<TrimestreId>();
  @Output() verTrimestre = new EventEmitter<TrimestreId>();

  readonly data = signal<FiscalPlazoActual | null>(null);
  readonly plazoId = signal<TrimestreId | null>(null);
  readonly oculto = signal(false);
  readonly loadError = signal(false);

  ngOnInit(): void {
    this.fiscal
      .getPlazoActual()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (d) => {
          this.data.set(d);
          const id = parseEtiquetaPlazo(d.trimestre);
          this.plazoId.set(id);
          if (id) {
            this.oculto.set(this.leidoOculto(id));
            this.plazoCargado.emit(id);
          }
        },
        error: () => this.loadError.set(true),
      });
  }

  visible(): boolean {
    return this.tieneMovimientos === true && !this.oculto() && this.data() != null && this.plazoId() != null;
  }

  clase(): string {
    const d = this.data();
    return d && plazoEnRojo(d.diasRestantes) ? 'fab fab--rojo' : 'fab fab--ambar';
  }

  texto(d: FiscalPlazoActual): string {
    const id = this.plazoId();
    const nombre = id ? etiquetaPlazo(id.trimestre) : d.trimestre;
    return `${nombre} · ${this.textoDias(d.diasRestantes)}`;
  }

  ver(): void {
    const id = this.plazoId();
    if (id) {
      this.verTrimestre.emit(id);
    }
  }

  ocultar(): void {
    const id = this.plazoId();
    if (id) {
      try {
        ocultarPlazo(id, localStorage);
      } catch {
        // Si el navegador bloquea el acceso, el aviso se oculta solo en esta visita.
      }
    }
    this.oculto.set(true);
  }

  private leidoOculto(id: TrimestreId): boolean {
    try {
      return plazoEstaOculto(id, localStorage);
    } catch {
      return false;
    }
  }

  private textoDias(dias: number): string {
    if (dias <= 0) {
      return 'vence hoy';
    }
    if (dias === 1) {
      return 'queda 1 día';
    }
    return `quedan ${dias} días`;
  }
}
