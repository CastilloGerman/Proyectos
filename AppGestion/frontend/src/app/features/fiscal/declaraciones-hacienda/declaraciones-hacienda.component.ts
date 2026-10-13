import { Component, DestroyRef, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelect, MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatAccordion, MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError } from 'rxjs';
import { FiscalService } from '../../../core/services/fiscal.service';
import { FiscalCriterio, Modelo303Resumen, Modelo347Resumen } from '../../../core/models/fiscal.model';
import { FiltroModelo303, observacionesModelo303 } from './hacienda-carga';
import { FiscalAlertaBannerComponent } from '../fiscal-alerta-banner/fiscal-alerta-banner.component';
import { TranslateService } from '@ngx-translate/core';

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
    MatTooltipModule,
    RouterLink,
    FiscalAlertaBannerComponent,
  ],
  templateUrl: './declaraciones-hacienda.component.html',
  styleUrl: './declaraciones-hacienda.component.scss',
})
export class DeclaracionesHaciendaComponent implements OnInit, OnDestroy {
  private readonly fiscal = inject(FiscalService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly title = inject(Title);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly carga303$ = new Subject<FiltroModelo303>();

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

  onYearChange(year: number): void {
    this.year = year;
    this.programarCarga303();
    this.cargar347();
  }

  onTrimestreChange(trimestre: number): void {
    this.trimestre = trimestre;
    this.programarCarga303();
  }

  onCriterioChange(criterio: FiscalCriterio): void {
    this.criterio = criterio;
    if (criterio === 'CAJA') {
      this.soloPagadas = false;
    }
    this.programarCarga303();
  }

  onSoloPagadasChange(soloPagadas: boolean): void {
    this.soloPagadas = soloPagadas;
    this.programarCarga303();
  }

  cambiarTrimestre(select: MatSelect): void {
    select.focus();
    select.open();
  }

  abrirExplicaciones(accordion: MatAccordion, host: HTMLElement): void {
    accordion.openAll();
    host.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  importeDestacado(valor: number): number {
    return Math.abs(valor ?? 0);
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
        this.snackBar.open(this.translate.instant('snack.tax347LoadFail'), this.translate.instant('common.close'), {
          duration: 4000,
        });
      },
    });
  }

  exportarPdf(): void {
    const r = this.resumen();
    if (!r) {
      this.snackBar.open(this.translate.instant('snack.taxQuarterFirst'), this.translate.instant('common.close'), {
        duration: 3000,
      });
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
      error: () =>
        this.snackBar.open(this.translate.instant('snack.taxPdfFail'), this.translate.instant('common.close'), {
          duration: 3000,
        }),
    });
  }

  ngOnInit(): void {
    this.title.setTitle('Hacienda');
    observacionesModelo303(this.carga303$, (filtro) => {
      this.loading.set(true);
      return this.fiscal.getModelo303(filtro.year, filtro.trimestre, filtro.criterio, filtro.soloPagadas).pipe(
        catchError((err) => {
          this.loading.set(false);
          this.avisarError303(err);
          return EMPTY;
        }),
      );
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((r) => {
        this.resumen.set(r);
        this.loading.set(false);
      });
    this.programarCarga303();
    this.cargar347();
  }

  ngOnDestroy(): void {
    this.title.setTitle(DEFAULT_DOC_TITLE);
  }

  private programarCarga303(): void {
    this.loading.set(true);
    this.carga303$.next({
      year: this.year,
      trimestre: this.trimestre,
      criterio: this.criterio,
      soloPagadas: this.soloPagadas,
    });
  }

  private avisarError303(err: { error?: { message?: unknown; detail?: unknown } }): void {
    const raw = err.error?.message ?? err.error?.detail;
    const msg =
      typeof raw === 'string' && String(raw).trim() !== ''
        ? String(raw).trim()
        : this.translate.instant('snack.tax303SummaryLoadFail');
    this.snackBar.open(msg, this.translate.instant('common.close'), { duration: 5000 });
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
