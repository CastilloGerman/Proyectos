import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { FiscalService } from '../../../core/services/fiscal.service';
import { FiscalCriterio, Modelo303Resumen, Modelo347Resumen } from '../../../core/models/fiscal.model';
import { FiscalAlertaBannerComponent } from '../fiscal-alerta-banner/fiscal-alerta-banner.component';

const DEFAULT_DOC_TITLE = 'Noemí - Web de Gestión';

@Component({
  selector: 'app-declaraciones-hacienda',
  imports: [
    FormsModule,
    DecimalPipe,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatRadioModule,
    MatCheckboxModule,
    MatTableModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatExpansionModule,
    FiscalAlertaBannerComponent,
  ],
  templateUrl: './declaraciones-hacienda.component.html',
  styleUrl: './declaraciones-hacienda.component.scss',
})
export class DeclaracionesHaciendaComponent implements OnInit, OnDestroy {
  private readonly fiscal = inject(FiscalService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly title = inject(Title);

  readonly loading = signal(false);
  readonly loading347 = signal(false);
  readonly resumen = signal<Modelo303Resumen | null>(null);
  readonly modelo347 = signal<Modelo347Resumen | null>(null);

  year = new Date().getFullYear();
  trimestre = this.trimestreActual();
  criterio: FiscalCriterio = 'DEVENGO';
  soloPagadas = false;

  readonly years = this.buildYears();
  readonly trimestres = [1, 2, 3, 4] as const;
  readonly displayedColumns347 = ['nombre', 'dni', 'base'] as const;

  cargar303(): void {
    this.loading.set(true);
    this.fiscal.getModelo303(this.year, this.trimestre, this.criterio, this.soloPagadas).subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message ?? err.error?.detail ?? 'No se pudo cargar el resumen';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }

  cargar347(): void {
    this.loading347.set(true);
    this.fiscal.getModelo347(this.year).subscribe({
      next: (r) => {
        this.modelo347.set(r);
        this.loading347.set(false);
      },
      error: () => {
        this.loading347.set(false);
        this.snackBar.open('Error al cargar el listado 347', 'Cerrar', { duration: 4000 });
      },
    });
  }

  exportarPdf(): void {
    const r = this.resumen();
    if (!r) {
      this.snackBar.open('Primero carga el resumen del trimestre', 'Cerrar', { duration: 3000 });
      return;
    }
    this.fiscal.downloadModelo303Pdf(this.year, this.trimestre, this.criterio, this.soloPagadas).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `modelo303-resumen-${this.year}-T${this.trimestre}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.snackBar.open('Error al generar el PDF', 'Cerrar', { duration: 3000 }),
    });
  }

  onCriterioChange(): void {
    if (this.criterio === 'CAJA') {
      this.soloPagadas = false;
    }
  }

  ngOnInit(): void {
    this.title.setTitle('Hacienda');
    this.cargar303();
    this.cargar347();
  }

  ngOnDestroy(): void {
    this.title.setTitle(DEFAULT_DOC_TITLE);
  }

  private trimestreActual(): number {
    const m = new Date().getMonth() + 1;
    if (m <= 3) return 1;
    if (m <= 6) return 2;
    if (m <= 9) return 3;
    return 4;
  }

  private buildYears(): number[] {
    const y = new Date().getFullYear();
    const out: number[] = [];
    for (let i = y + 1; i >= y - 6; i--) {
      out.push(i);
    }
    return out;
  }
}
