import { Component, OnInit, inject, signal } from '@angular/core';
import { ThemeService } from '../../../core/theme/theme.service';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService, UsuarioResponse } from '../../../core/auth/auth.service';
import {
  CondicionesPresupuestoFormValue,
  PresupuestoCondicionDisponible,
} from '../../../core/models/presupuesto-condiciones.model';
import { PresupuestoService } from '../../../core/services/presupuesto.service';
import { CondicionesPresupuestoComponent } from '../../presupuestos/condiciones-presupuesto/condiciones-presupuesto.component';
import { CURRENCY_OPTIONS, LOCALE_OPTIONS, TIMEZONE_OPTIONS } from './preferencias-options';

@Component({
    selector: 'app-preferencias',
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        RouterLink,
        CondicionesPresupuestoComponent,
        MatCardModule,
        MatFormFieldModule,
        MatSelectModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatSlideToggleModule,
        MatDividerModule,
    ],
    templateUrl: './preferencias.component.html',
    styleUrl: './preferencias.component.scss'
})
export class PreferenciasComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly presupuestoService = inject(PresupuestoService);
  private readonly snackBar = inject(MatSnackBar);
  readonly theme = inject(ThemeService);

  readonly locales = LOCALE_OPTIONS;
  /** Incluye moneda guardada aunque no esté en la lista corta. */
  readonly currencyOptions = signal<ReadonlyArray<{ value: string; label: string }>>(CURRENCY_OPTIONS);
  /** Incluye la zona guardada aunque no esté en la lista corta. */
  readonly timezoneOptions = signal<ReadonlyArray<{ value: string; label: string }>>(TIMEZONE_OPTIONS);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly savingCondiciones = signal(false);
  readonly loadError = signal(false);
  readonly me = signal<UsuarioResponse | null>(null);
  /** Catálogo de condiciones (API); mismas claves que en el formulario de presupuesto. */
  readonly condicionesCatalogo = signal<PresupuestoCondicionDisponible[]>([]);
  /** Evita spinner infinito si la petición falla o devuelve []. */
  readonly condicionesPresupuestosListo = signal(false);
  /** Valor del CVA (solo se usan las claves activas; nota siempre vacía aquí). */
  condicionesPredModel: CondicionesPresupuestoFormValue = { condicionesActivas: [], notaAdicional: '' };

  readonly form = this.fb.nonNullable.group({
    locale: ['es', Validators.required],
    timeZone: ['Europe/Madrid', Validators.required],
    currencyCode: ['EUR', [Validators.required, Validators.pattern(/^[A-Z]{3}$/i)]],
  });

  ngOnInit(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.auth.refreshUser().subscribe({
      next: (data) => {
        if (data) {
          this.me.set(data);
          const tz = data.timeZone ?? 'Europe/Madrid';
          let tzOpts: ReadonlyArray<{ value: string; label: string }> = TIMEZONE_OPTIONS;
          if (!TIMEZONE_OPTIONS.some((o) => o.value === tz)) {
            tzOpts = [{ value: tz, label: `${tz} (guardada)` }, ...TIMEZONE_OPTIONS];
          }
          this.timezoneOptions.set(tzOpts);
          const cur = (data.currencyCode ?? 'EUR').toUpperCase();
          let curOpts: ReadonlyArray<{ value: string; label: string }> = CURRENCY_OPTIONS;
          if (!CURRENCY_OPTIONS.some((o) => o.value === cur)) {
            curOpts = [{ value: cur, label: `${cur} (guardada)` }, ...CURRENCY_OPTIONS];
          }
          this.currencyOptions.set(curOpts);
          this.form.patchValue({
            locale: data.locale ?? 'es',
            timeZone: tz,
            currencyCode: cur,
          });
          this.form.markAsPristine();
          this.condicionesPresupuestosListo.set(false);
          this.presupuestoService.getCondicionesDisponibles().subscribe({
            next: (cat) => {
              this.condicionesCatalogo.set(cat);
              this.condicionesPredModel = {
                condicionesActivas: [...(data.condicionesPresupuestoPredeterminadas ?? [])],
                notaAdicional: '',
              };
              this.condicionesPresupuestosListo.set(true);
            },
            error: () => {
              this.condicionesCatalogo.set([]);
              this.condicionesPresupuestosListo.set(true);
            },
          });
        } else {
          this.loadError.set(true);
        }
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  guardar(): void {
    if (this.form.invalid || this.form.pristine) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.auth
      .updatePreferences({
        locale: v.locale,
        timeZone: v.timeZone,
        currencyCode: v.currencyCode.toUpperCase(),
      })
      .subscribe({
        next: (updated) => {
          this.me.set(updated);
          this.form.patchValue(
            {
              locale: updated.locale ?? 'es',
              timeZone: updated.timeZone ?? 'Europe/Madrid',
              currencyCode: (updated.currencyCode ?? 'EUR').toUpperCase(),
            },
            { emitEvent: false }
          );
          this.form.markAsPristine();
          this.snackBar.open('Preferencias guardadas', 'Cerrar', { duration: 3000 });
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          const msg = err.error?.message || err.error?.detail || 'No se pudo guardar';
          this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
        },
      });
  }

  reintentar(): void {
    this.ngOnInit();
  }

  guardarCondicionesPredeterminadas(): void {
    if (this.savingCondiciones()) return;
    this.savingCondiciones.set(true);
    this.presupuestoService.guardarMisCondicionesPredeterminadas(this.condicionesPredModel.condicionesActivas).subscribe({
      next: () => {
        this.auth.refreshUser().subscribe({
          next: (u) => {
            if (u) {
              this.me.set(u);
            }
            this.snackBar.open('Condiciones por defecto guardadas', 'Cerrar', { duration: 3000 });
            this.savingCondiciones.set(false);
          },
          error: () => {
            this.savingCondiciones.set(false);
            this.snackBar.open('Guardado; no se pudo refrescar el perfil', 'Cerrar', { duration: 4000 });
          },
        });
      },
      error: (err) => {
        this.savingCondiciones.set(false);
        const msg = err.error?.message || err.error?.detail || 'No se pudo guardar';
        this.snackBar.open(msg, 'Cerrar', { duration: 5000 });
      },
    });
  }
}
