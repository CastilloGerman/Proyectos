import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule, MAT_DATE_LOCALE } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { AuthService, UsuarioResponse } from '../../../core/auth/auth.service';
import { getApiErrorMessage } from '../../../core/http/api-error.util';
import { buildPaisOptionsEs } from '../../../shared/constants/paises-select-options';
import { formatLocalDateForApi, parseApiLocalDate } from '../../../shared/utils/local-date.util';

const PHONE_CHARS = /^[\d\s+().-]*$/;

/** Required real: Angular `required` considera válido un string solo con espacios. */
function nombrePerfilRequerido(c: AbstractControl): ValidationErrors | null {
  const t = ((c.value as string) ?? '').trim();
  return t.length === 0 ? { required: true } : null;
}

function fechaNacimientoOpcionalValida(c: AbstractControl): ValidationErrors | null {
  const v = c.value as Date | null;
  if (v == null) {
    return null;
  }
  const endOfToday = new Date();
  endOfToday.setHours(23, 59, 59, 999);
  if (v.getTime() > endOfToday.getTime()) {
    return { futureDate: true };
  }
  if (v.getTime() < new Date(1900, 0, 1).getTime()) {
    return { tooOld: true };
  }
  return null;
}

@Component({
    selector: 'app-perfil',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
        MatDividerModule,
        MatDatepickerModule,
        MatNativeDateModule,
        MatSelectModule,
    ],
    providers: [{ provide: MAT_DATE_LOCALE, useValue: 'es-ES' }],
    templateUrl: './perfil.component.html',
    styleUrl: './perfil.component.scss'
})
export class PerfilComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly loadError = signal(false);
  /** Datos de solo lectura procedentes de GET /auth/me (fecha alta, rol en servidor, etc.). */
  readonly me = signal<UsuarioResponse | null>(null);

  readonly paisOptions = buildPaisOptionsEs();
  readonly hoy = new Date();

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.maxLength(100), nombrePerfilRequerido]],
    telefono: ['', [Validators.maxLength(30), Validators.pattern(PHONE_CHARS)]],
    fechaNacimiento: [null as Date | null, [fechaNacimientoOpcionalValida]],
    genero: [''],
    nacionalidadIso: [''],
    paisResidenciaIso: [''],
  });

  readonly emailDisplay = computed(() => this.me()?.email ?? this.auth.user()?.email ?? '');

  readonly fechaAltaFmt = computed(() => {
    const raw = this.me()?.fechaCreacion;
    if (!raw) return '—';
    const d = new Date(raw);
    return Number.isNaN(d.getTime())
      ? '—'
      : d.toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });
  });

  readonly rolEtiqueta = computed(() => {
    const r = (this.me()?.rol ?? this.auth.user()?.rol ?? 'USER').toUpperCase();
    if (r === 'ADMIN') return 'Administrador';
    return 'Usuario';
  });

  readonly estadoSuscripcion = computed(() => {
    const s = this.me()?.subscriptionStatus ?? this.auth.user()?.subscriptionStatus;
    if (!s) return '—';
    const map: Record<string, string> = {
      TRIAL_ACTIVE: 'Prueba activa',
      TRIAL_EXPIRED: 'Prueba finalizada',
      ACTIVE: 'Suscripción activa',
      PAST_DUE: 'Pago pendiente',
      CANCELED: 'Cancelada',
      EXPIRED: 'Expirada',
    };
    return map[s] ?? s;
  });

  ngOnInit(): void {
    this.loadError.set(false);
    this.loading.set(true);
    this.auth.refreshUser().subscribe({
      next: (data) => {
        if (data) {
          this.me.set(data);
          this.form.patchValue({
            nombre: data.nombre ?? '',
            telefono: data.telefono ?? '',
            fechaNacimiento: parseApiLocalDate(data.fechaNacimiento ?? undefined),
            genero: data.genero ?? '',
            nacionalidadIso: data.nacionalidadIso ?? '',
            paisResidenciaIso: data.paisResidenciaIso ?? '',
          });
          this.form.markAsPristine();
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
    const { nombre, telefono, fechaNacimiento, genero, nacionalidadIso, paisResidenciaIso } =
      this.form.getRawValue();
    const nombreTrim = nombre.trim().replace(/\s+/g, ' ');
    if (!nombreTrim) {
      this.form.get('nombre')?.setErrors({ required: true });
      return;
    }

    const fechaStr = fechaNacimiento ? formatLocalDateForApi(fechaNacimiento) : null;
    const gen = genero.trim();
    const nat = nacionalidadIso.trim().toUpperCase();
    const res = paisResidenciaIso.trim().toUpperCase();

    this.saving.set(true);
    this.auth
      .updateProfile({
        nombre: nombreTrim,
        telefono: telefono.trim(),
        fechaNacimiento: fechaStr,
        genero: gen.length > 0 ? gen : null,
        nacionalidadIso: nat.length === 2 ? nat : null,
        paisResidenciaIso: res.length === 2 ? res : null,
      })
      .subscribe({
        next: (updated) => {
          this.me.set(updated);
          this.form.patchValue(
            {
              nombre: updated.nombre ?? '',
              telefono: updated.telefono ?? '',
              fechaNacimiento: parseApiLocalDate(updated.fechaNacimiento ?? undefined),
              genero: updated.genero ?? '',
              nacionalidadIso: updated.nacionalidadIso ?? '',
              paisResidenciaIso: updated.paisResidenciaIso ?? '',
            },
            { emitEvent: false }
          );
          this.form.markAsPristine();
          this.snackBar.open('Cambios guardados correctamente', 'Cerrar', { duration: 3000 });
          this.saving.set(false);
        },
        error: (err) => {
          this.saving.set(false);
          this.snackBar.open(getApiErrorMessage(err, 'No se pudo guardar el perfil'), 'Cerrar', { duration: 6000 });
        },
      });
  }

  reintentar(): void {
    this.ngOnInit();
  }
}
