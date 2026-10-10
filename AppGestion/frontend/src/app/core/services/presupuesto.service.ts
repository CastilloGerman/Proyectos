import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PresupuestoCondicionDisponible } from '../models/presupuesto-condiciones.model';
import { AnticipoRegistroRequest, AnticipoResumen, Presupuesto, PresupuestoEnlaceCreado, PresupuestoEnlaceEstado, PresupuestoPublico, PresupuestoRequest, PresupuestoRespuestaClienteRequest } from '../models/presupuesto.model';
import { Factura } from '../models/factura.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PresupuestoService {
  private readonly apiUrl = `${environment.apiUrl}/presupuestos`;

  constructor(private http: HttpClient) {}

  createFacturaFromPresupuesto(presupuestoId: number): Observable<Factura> {
    return this.http.post<Factura>(`${this.apiUrl}/${presupuestoId}/factura`, {});
  }

  /** Factura de venta principal cuando el presupuesto tiene anticipo ya facturado. */
  createFacturaFinalFromPresupuesto(presupuestoId: number): Observable<Factura> {
    return this.http.post<Factura>(`${this.apiUrl}/${presupuestoId}/factura-final`, {});
  }

  registrarAnticipo(presupuestoId: number, body: AnticipoRegistroRequest): Observable<Presupuesto> {
    return this.http.post<Presupuesto>(`${this.apiUrl}/${presupuestoId}/anticipo`, body);
  }

  generarFacturaAnticipo(presupuestoId: number): Observable<Factura> {
    return this.http.post<Factura>(`${this.apiUrl}/${presupuestoId}/factura-anticipo`, {});
  }

  getResumenAnticipo(presupuestoId: number): Observable<AnticipoResumen> {
    return this.http.get<AnticipoResumen>(`${this.apiUrl}/${presupuestoId}/resumen-anticipo`);
  }

  getAll(q?: string): Observable<Presupuesto[]> {
    const params = q ? { params: { q } } : {};
    return this.http.get<Presupuesto[]>(this.apiUrl, params);
  }

  getById(id: number): Observable<Presupuesto> {
    return this.http.get<Presupuesto>(`${this.apiUrl}/${id}`);
  }

  create(data: PresupuestoRequest): Observable<Presupuesto> {
    return this.http.post<Presupuesto>(this.apiUrl, data);
  }

  update(id: number, data: PresupuestoRequest): Observable<Presupuesto> {
    return this.http.put<Presupuesto>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, { responseType: 'blob' });
  }

  enviarPorEmail(id: number, request?: { email?: string; asunto?: string; mensaje?: string }): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/enviar-email`, request ?? {});
  }

  marcarEnviado(id: number, canal: 'WHATSAPP' | 'EMAIL'): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/marcar-enviado`, { canal });
  }

  crearEnlace(id: number): Observable<PresupuestoEnlaceCreado> {
    return this.http.post<PresupuestoEnlaceCreado>(`${this.apiUrl}/${id}/enlace`, {});
  }

  regenerarEnlace(id: number): Observable<PresupuestoEnlaceCreado> {
    return this.http.post<PresupuestoEnlaceCreado>(`${this.apiUrl}/${id}/enlace/regenerar`, {});
  }

  revocarEnlace(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/enlace`);
  }

  estadoEnlace(id: number): Observable<PresupuestoEnlaceEstado> {
    return this.http.get<PresupuestoEnlaceEstado>(`${this.apiUrl}/${id}/enlace/estado`);
  }

  silenciarSeguimiento(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/seguimiento/silenciar`, {});
  }

  reactivarSeguimiento(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}/seguimiento/silenciar`);
  }

  getPublico(token: string): Observable<PresupuestoPublico> {
    return this.http.get<PresupuestoPublico>(`${environment.apiUrl}/publico/presupuestos/${encodeURIComponent(token)}`);
  }

  marcarPublicoVisto(token: string): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/publico/presupuestos/${encodeURIComponent(token)}/visto`, {});
  }

  responderPublico(token: string, body: PresupuestoRespuestaClienteRequest): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/publico/presupuestos/${encodeURIComponent(token)}/responder`, body);
  }

  descargarPdfPublico(token: string): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/publico/presupuestos/${encodeURIComponent(token)}/pdf`, { responseType: 'blob' });
  }

  /** Textos y claves del catálogo (única fuente de verdad en API). */
  getCondicionesDisponibles(): Observable<PresupuestoCondicionDisponible[]> {
    return this.http.get<PresupuestoCondicionDisponible[]>(`${this.apiUrl}/condiciones-disponibles`);
  }

  /** Condiciones marcadas por defecto para presupuestos nuevos (perfil). */
  getMisCondicionesPredeterminadas(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/mis-condiciones-predeterminadas`);
  }

  guardarMisCondicionesPredeterminadas(claves: string[]): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/mis-condiciones-predeterminadas`, { claves });
  }

}
