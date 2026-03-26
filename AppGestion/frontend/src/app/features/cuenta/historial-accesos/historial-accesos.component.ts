import {
  ApplicationRef,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/auth/auth.service';
import {
  AuditAccessApiService,
  AuditAccessEventDto,
  AuditAccessPageDto,
} from '../../../core/services/audit-access-api.service';
import { getApiErrorMessage } from '../../../core/http/api-error.util';

const EVENT_TYPES: { value: string; label: string }[] = [
  { value: 'LOGIN_SUCCESS', label: 'Inicio de sesión correcto' },
  { value: 'LOGIN_FAILURE', label: 'Inicio de sesión fallido' },
  { value: 'LOGOUT', label: 'Cierre de sesión' },
  { value: 'REGISTER_SUCCESS', label: 'Registro de cuenta' },
  { value: 'INVITE_ACCEPT_SUCCESS', label: 'Alta por invitación' },
  { value: 'PASSWORD_CHANGE_SUCCESS', label: 'Contraseña cambiada' },
  { value: 'PASSWORD_CHANGE_FAILURE', label: 'Cambio de contraseña fallido' },
  { value: 'PASSWORD_RESET_REQUESTED', label: 'Solicitud recuperación contraseña' },
  { value: 'PASSWORD_RESET_COMPLETED', label: 'Contraseña restablecida (enlace)' },
  { value: 'SESSION_REVOKED_DEVICE', label: 'Sesión cerrada (otro dispositivo)' },
  { value: 'SESSION_REVOKED_OTHERS', label: 'Cerrar otras sesiones' },
  { value: 'TOTP_ENABLED', label: '2FA activado' },
  { value: 'TOTP_DISABLED', label: '2FA desactivado' },
  { value: 'TOTP_SETUP_CANCELLED', label: 'Configuración 2FA cancelada' },
  { value: 'AUDIT_EXPORT_JSON', label: 'Exportación auditoría (JSON)' },
  { value: 'AUDIT_EXPORT_CSV', label: 'Exportación auditoría (CSV)' },
];

@Component({
    selector: 'app-historial-accesos',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        RouterLink,
        DatePipe,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatPaginatorModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatTooltipModule,
        MatDividerModule,
        MatProgressSpinnerModule,
        MatSnackBarModule,
    ],
    templateUrl: './historial-accesos.component.html',
    styleUrl: './historial-accesos.component.scss'
})
export class HistorialAccesosComponent implements OnInit {
  private readonly api = inject(AuditAccessApiService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly appRef = inject(ApplicationRef);

  readonly eventTypes = EVENT_TYPES;

  /** Filas de la página actual (tabla HTML nativa, sin mat-table/CDK). */
  readonly tableRows = signal<AuditAccessEventDto[]>([]);

  readonly loading = signal(false);
  readonly loadError = signal(false);
  readonly pageMeta = signal<AuditAccessPageDto | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly expandedId = signal<number | null>(null);
  readonly exporting = signal(false);

  readonly isAdmin = computed(() => this.auth.businessRole() === 'ADMIN');

  readonly filters = this.fb.nonNullable.group({
    fromDate: [''],
    toDate: [''],
    eventType: [''],
    success: ['' as '' | 'true' | 'false'],
    ip: [''],
    q: [''],
    usuarioId: [''],
  });

  ngOnInit(): void {
    this.cargar();
  }

  eventoEtiqueta(code: string): string {
    return EVENT_TYPES.find((e) => e.value === code)?.label ?? code;
  }

  cargar(): void {
    this.loading.set(true);
    this.loadError.set(false);
    const f = this.filters.getRawValue();
    const usuarioIdRaw = f.usuarioId.trim();
    const usuarioId =
      this.isAdmin() && usuarioIdRaw ? Number.parseInt(usuarioIdRaw, 10) : null;
    const q = {
      page: this.pageIndex(),
      size: this.pageSize(),
      from: toInstantStart(f.fromDate),
      to: toInstantEnd(f.toDate),
      eventType: f.eventType || null,
      success: f.success === '' ? null : f.success === 'true',
      ip: f.ip.trim() || null,
      q: f.q.trim() || null,
      usuarioId: usuarioId != null && !Number.isNaN(usuarioId) ? usuarioId : null,
    };
    this.api.list(q).subscribe({
      next: (page) => {
        this.tableRows.set([...page.content]);
        this.pageMeta.set(page);
        this.loading.set(false);
        this.appRef.tick();
      },
      error: (err) => {
        this.loadError.set(true);
        this.loading.set(false);
        this.snackBar.open(getApiErrorMessage(err, 'No se pudo cargar el historial'), 'Cerrar', { duration: 6000 });
      },
    });
  }

  aplicarFiltros(): void {
    this.pageIndex.set(0);
    this.cargar();
  }

  limpiarFiltros(): void {
    this.filters.reset({
      fromDate: '',
      toDate: '',
      eventType: '',
      success: '',
      ip: '',
      q: '',
      usuarioId: '',
    });
    this.pageIndex.set(0);
    this.cargar();
  }

  onPage(ev: PageEvent): void {
    this.pageIndex.set(ev.pageIndex);
    this.pageSize.set(ev.pageSize);
    this.cargar();
  }

  toggleExpand(row: AuditAccessEventDto): void {
    this.expandedId.update((id) => (id === row.id ? null : row.id));
  }

  metadataPretty(json: string | null | undefined): string {
    if (!json) return '—';
    try {
      return JSON.stringify(JSON.parse(json), null, 2);
    } catch {
      return json;
    }
  }

  exportar(format: 'csv' | 'json'): void {
    const f = this.filters.getRawValue();
    const usuarioIdRaw = f.usuarioId.trim();
    const usuarioId =
      this.isAdmin() && usuarioIdRaw ? Number.parseInt(usuarioIdRaw, 10) : null;
    const q = {
      from: toInstantStart(f.fromDate),
      to: toInstantEnd(f.toDate),
      eventType: f.eventType || null,
      success: f.success === '' ? null : f.success === 'true',
      ip: f.ip.trim() || null,
      q: f.q.trim() || null,
      usuarioId: usuarioId != null && !Number.isNaN(usuarioId) ? usuarioId : null,
    };
    this.exporting.set(true);
    this.api.exportBlob(format, q).subscribe({
      next: (res) => {
        this.exporting.set(false);
        const blob = res.body;
        if (!blob) {
          this.snackBar.open('Exportación vacía', 'Cerrar', { duration: 4000 });
          return;
        }
        const cd = res.headers.get('Content-Disposition');
        const name = parseFilename(cd) ?? `historial-accesos.${format}`;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = name;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.exporting.set(false);
        this.snackBar.open(getApiErrorMessage(err, 'No se pudo exportar'), 'Cerrar', { duration: 6000 });
      },
    });
  }
}

function toInstantStart(dateStr: string): string | null {
  const t = dateStr?.trim();
  if (!t) return null;
  return `${t}T00:00:00.000Z`;
}

function toInstantEnd(dateStr: string): string | null {
  const t = dateStr?.trim();
  if (!t) return null;
  return `${t}T23:59:59.999Z`;
}

function parseFilename(cd: string | null): string | undefined {
  if (!cd) return undefined;
  const star = cd.match(/filename\*=UTF-8''([^;]+)/i);
  if (star) {
    try {
      return decodeURIComponent(star[1].trim());
    } catch {
      return star[1].trim();
    }
  }
  const m = cd.match(/filename="([^"]+)"/i);
  if (m) return m[1];
  const m2 = cd.match(/filename=([^;]+)/i);
  return m2 ? m2[1].trim().replace(/^["']|["']$/g, '') : undefined;
}
