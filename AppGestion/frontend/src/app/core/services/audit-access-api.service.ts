import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuditAccessEventDto {
  id: number;
  occurredAt: string;
  usuarioId: number | null;
  userEmail: string | null;
  eventType: string;
  success: boolean;
  failureReason: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  countryCode: string | null;
  sessionId: string | null;
  resourcePath: string | null;
  traceId: string | null;
  sensitive: boolean;
  metadataJson: string | null;
}

export interface AuditAccessPageDto {
  content: AuditAccessEventDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  failedCount24h: number;
  sensitiveCount24h: number;
}

export interface AuditAccessQuery {
  page?: number;
  size?: number;
  from?: string | null;
  to?: string | null;
  eventType?: string | null;
  success?: boolean | null;
  ip?: string | null;
  q?: string | null;
  usuarioId?: number | null;
}

@Injectable({ providedIn: 'root' })
export class AuditAccessApiService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/auth/audit-access`;

  list(q: AuditAccessQuery): Observable<AuditAccessPageDto> {
    return this.http.get<AuditAccessPageDto>(this.base, { params: this.toParams(q) });
  }

  exportBlob(format: 'csv' | 'json', q: AuditAccessQuery): Observable<HttpResponse<Blob>> {
    let p = this.toParams(q);
    p = p.set('format', format);
    return this.http.get(`${this.base}/export`, {
      params: p,
      observe: 'response',
      responseType: 'blob',
    });
  }

  private toParams(q: AuditAccessQuery): HttpParams {
    let p = new HttpParams();
    if (q.page != null) p = p.set('page', String(q.page));
    if (q.size != null) p = p.set('size', String(q.size));
    if (q.from) p = p.set('from', q.from);
    if (q.to) p = p.set('to', q.to);
    if (q.eventType) p = p.set('eventType', q.eventType);
    if (q.success === true || q.success === false) p = p.set('success', String(q.success));
    if (q.ip?.trim()) p = p.set('ip', q.ip.trim());
    if (q.q?.trim()) p = p.set('q', q.q.trim());
    if (q.usuarioId != null && q.usuarioId > 0) p = p.set('usuarioId', String(q.usuarioId));
    return p;
  }
}
