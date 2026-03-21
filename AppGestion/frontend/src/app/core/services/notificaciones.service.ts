import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export type NotificacionTipo = 'SISTEMA' | 'SUSCRIPCION' | 'FACTURACION_CLIENTE';
export type NotificacionSeveridad = 'INFO' | 'WARNING' | 'ERROR';

export interface NotificacionDto {
  id: number;
  tipo: NotificacionTipo;
  severidad: NotificacionSeveridad;
  titulo: string;
  resumen: string | null;
  leida: boolean;
  actionPath: string | null;
  createdAt: string;
}

export interface PageNotificaciones {
  content: NotificacionDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type FiltroLectura = 'todas' | 'no_leidas' | 'leidas';

@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/auth/notifications`;

  /** Contador para la campana (se actualiza con refreshUnreadCount). */
  readonly unreadCount = signal(0);

  refreshUnreadCount(): void {
    this.http.get<{ count: number }>(`${this.base}/unread-count`).subscribe({
      next: (r) => this.unreadCount.set(Math.max(0, r.count ?? 0)),
      error: () => this.unreadCount.set(0),
    });
  }

  list(page: number, size: number, filtro: FiltroLectura): Observable<PageNotificaciones> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (filtro === 'no_leidas') {
      params = params.set('read', 'false');
    } else if (filtro === 'leidas') {
      params = params.set('read', 'true');
    }
    return this.http.get<PageNotificaciones>(this.base, { params });
  }

  markRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/${id}/read`, {}).pipe(
      tap(() => this.refreshUnreadCount())
    );
  }

  markAllRead(): Observable<{ updated: number }> {
    return this.http.post<{ updated: number }>(`${this.base}/read-all`, {}).pipe(
      tap(() => this.refreshUnreadCount())
    );
  }
}
