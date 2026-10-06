import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, Observable, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PresupuestoIaBorradorRequest,
  PresupuestoIaBorradorResponse,
  PresupuestoIaRequestError,
} from '../models/presupuesto-ia.model';

@Injectable({ providedIn: 'root' })
export class PresupuestoIaService {
  private readonly apiUrl = `${environment.apiUrl}/presupuestos/ia/borrador`;

  constructor(private http: HttpClient) {}

  generarBorrador(request: PresupuestoIaBorradorRequest): Observable<PresupuestoIaBorradorResponse> {
    return this.http.post<PresupuestoIaBorradorResponse>(this.apiUrl, request).pipe(
      catchError((error: unknown) => throwError(() => mapPresupuestoIaError(error))),
    );
  }
}

export function mapPresupuestoIaError(error: unknown): PresupuestoIaRequestError {
  if (!(error instanceof HttpErrorResponse)) {
    return new PresupuestoIaRequestError('unknown', null);
  }
  const body = error.error as { message?: unknown; detail?: unknown } | null;
  const message = typeof body?.message === 'string'
    ? body.message
    : typeof body?.detail === 'string' ? body.detail : null;
  const normalized = normalizeMessage(message ?? '');
  const status = error.status;

  if (status === 400 && /(8000|8000 caracteres|demasiado larga|superar)/.test(normalized)) {
    return new PresupuestoIaRequestError('too-long', status, message);
  }
  if (status === 401) return new PresupuestoIaRequestError('unauthorized', status, message);
  if (status === 403) return new PresupuestoIaRequestError('forbidden', status, message);
  if (status === 429) {
    if (normalized.includes('diario')) return new PresupuestoIaRequestError('quota-daily', status, message);
    if (normalized.includes('intentos')) return new PresupuestoIaRequestError('quota-attempts', status, message);
    if (normalized.includes('cuota')) return new PresupuestoIaRequestError('quota-provider', status, message);
    return new PresupuestoIaRequestError('quota-hourly', status, message);
  }
  if (status === 503 && normalized.includes('desactivado')) {
    return new PresupuestoIaRequestError('disabled', status, message);
  }
  if (status === 503 || status === 502 || status === 504 || status === 0 || status >= 500) {
    return new PresupuestoIaRequestError('provider', status, message);
  }
  if (status === 400) return new PresupuestoIaRequestError('invalid-request', status, message);
  return new PresupuestoIaRequestError('unknown', status, message);
}

function normalizeMessage(message: string): string {
  return message.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase();
}
