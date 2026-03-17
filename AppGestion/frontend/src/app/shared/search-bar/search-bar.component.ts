import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  Subject, combineLatest, of
} from 'rxjs';
import {
  debounceTime, distinctUntilChanged, switchMap,
  catchError, takeUntil, map
} from 'rxjs/operators';
import { FacturaService } from '../../core/services/factura.service';
import { PresupuestoService } from '../../core/services/presupuesto.service';
import { ClienteService } from '../../core/services/cliente.service';

export interface SearchResult {
  type: 'factura' | 'presupuesto' | 'cliente';
  label: string;
  sublabel?: string;
  id: number;
}

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="search-wrap">
      <div class="search-box">
        <mat-icon class="search-icon">search</mat-icon>
        <mat-form-field appearance="outline" class="search-field">
          <input
            matInput
            type="search"
            placeholder="Buscar factura, cliente, presupuesto…"
            [formControl]="searchCtrl"
            [matAutocomplete]="auto"
            (focus)="onFocus()"
          >
          @if (loading) {
            <mat-spinner matSuffix diameter="18"></mat-spinner>
          }
        </mat-form-field>
      </div>

      <mat-autocomplete #auto="matAutocomplete" (optionSelected)="onSelect($event.option.value)">
        @if (results.length === 0 && (searchCtrl.value?.length ?? 0) >= 2 && !loading) {
          <mat-option disabled>Sin resultados</mat-option>
        }
        @for (group of groupedResults; track group.type) {
          <mat-optgroup [label]="group.label">
            @for (r of group.items; track r.id) {
              <mat-option [value]="r">
                <span class="result-label">{{ r.label }}</span>
                @if (r.sublabel) {
                  <span class="result-sub">{{ r.sublabel }}</span>
                }
              </mat-option>
            }
          </mat-optgroup>
        }
      </mat-autocomplete>
    </div>
  `,
  styles: [`
    .search-wrap {
      display: flex;
      align-items: center;
    }

    .search-box {
      display: flex;
      align-items: center;
      width: 420px;
      min-width: 280px;
      max-width: 100%;
      background: rgba(255, 255, 255, 0.15);
      border-radius: var(--app-radius-md, 12px);
      border: 1px solid rgba(255, 255, 255, 0.5);
      overflow: hidden;
    }

    .search-icon {
      flex-shrink: 0;
      margin: 0;
      padding: 0 8px;
      color: white;
      font-size: 24px;
      width: 24px;
      height: 24px;
    }

    .search-field {
      flex: 1;
      min-width: 0;
    }

    .search-field ::ng-deep .mat-mdc-form-field-subscript-wrapper {
      display: none;
    }

    .search-field ::ng-deep .mat-mdc-text-field-wrapper {
      background: transparent !important;
    }

    .search-field ::ng-deep .mat-mdc-form-field-infix {
      padding-top: 12px;
      padding-bottom: 12px;
      padding-left: 4px !important;
      padding-right: 12px !important;
    }

    .search-field ::ng-deep .mdc-notched-outline .mdc-notched-outline__leading,
    .search-field ::ng-deep .mdc-notched-outline .mdc-notched-outline__notch,
    .search-field ::ng-deep .mdc-notched-outline .mdc-notched-outline__trailing {
      border: none !important;
    }

    .search-field ::ng-deep input,
    .search-field ::ng-deep .mat-mdc-input {
      color: white;
      text-align: left !important;
      padding-left: 0 !important;
    }

    .search-field ::ng-deep input::placeholder {
      color: rgba(255, 255, 255, 0.7);
      text-align: left !important;
    }

    .search-field ::ng-deep .mat-mdc-form-field-outline {
      color: transparent !important;
    }

    .result-label {
      font-weight: 500;
    }

    .result-sub {
      font-size: 0.8rem;
      color: rgba(0, 0, 0, 0.55);
      margin-left: 8px;
    }
  `],
})
export class SearchBarComponent implements OnDestroy {
  searchCtrl = new FormControl('');
  results: SearchResult[] = [];
  groupedResults: { type: string; label: string; items: SearchResult[] }[] = [];
  loading = false;

  private destroy$ = new Subject<void>();

  constructor(
    private facturaService: FacturaService,
    private presupuestoService: PresupuestoService,
    private clienteService: ClienteService,
    private router: Router,
  ) {
    this.searchCtrl.valueChanges.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
      switchMap((q) => {
        if (!q || q.length < 2) {
          this.results = [];
          this.groupedResults = [];
          return of(null);
        }
        this.loading = true;
        return combineLatest([
          this.facturaService.getAll(q).pipe(catchError(() => of([]))),
          this.presupuestoService.getAll(q).pipe(catchError(() => of([]))),
          this.clienteService.getAll(q).pipe(catchError(() => of([]))),
        ]).pipe(
          map(([facturas, presupuestos, clientes]) => ({ facturas, presupuestos, clientes }))
        );
      })
    ).subscribe((data) => {
      this.loading = false;
      if (!data) return;
      const { facturas, presupuestos, clientes } = data;
      const facturaResults: SearchResult[] = facturas.slice(0, 5).map((f) => ({
        type: 'factura' as const,
        label: `${f.numeroFactura} · ${f.clienteNombre}`,
        sublabel: `${f.total.toFixed(2)} € · ${f.estadoPago}`,
        id: f.id,
      }));
      const presupuestoResults: SearchResult[] = presupuestos.slice(0, 5).map((p) => ({
        type: 'presupuesto' as const,
        label: p.clienteNombre,
        sublabel: `${p.total.toFixed(2)} € · ${p.estado}`,
        id: p.id,
      }));
      const clienteResults: SearchResult[] = clientes.slice(0, 5).map((c) => ({
        type: 'cliente' as const,
        label: c.nombre,
        sublabel: c.email ?? undefined,
        id: c.id,
      }));

      this.results = [...facturaResults, ...presupuestoResults, ...clienteResults];
      this.groupedResults = [
        facturaResults.length ? { type: 'factura', label: 'Facturas', items: facturaResults } : null,
        presupuestoResults.length ? { type: 'presupuesto', label: 'Presupuestos', items: presupuestoResults } : null,
        clienteResults.length ? { type: 'cliente', label: 'Clientes', items: clienteResults } : null,
      ].filter(Boolean) as { type: string; label: string; items: SearchResult[] }[];
    });
  }

  onFocus(): void {
    // trigger search again if there's already a value
    const v = this.searchCtrl.value;
    if (v && v.length >= 2) this.searchCtrl.setValue(v);
  }

  onSelect(result: SearchResult): void {
    const routes: Record<string, string> = {
      factura: '/facturas',
      presupuesto: '/presupuestos',
      cliente: '/clientes',
    };
    this.router.navigate([routes[result.type], result.id]);
    this.searchCtrl.setValue('', { emitEvent: false });
    this.results = [];
    this.groupedResults = [];
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
