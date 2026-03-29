import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Factura, FacturaRequest, FacturaCobroRequest } from '../models/factura.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FacturaService {
  private readonly apiUrl = `${environment.apiUrl}/facturas`;

  constructor(private http: HttpClient) {}

  getAll(q?: string): Observable<Factura[]> {
    const params = q ? { params: { q } } : {};
    return this.http.get<Factura[]>(this.apiUrl, params);
  }

  getById(id: number): Observable<Factura> {
    return this.http.get<Factura>(`${this.apiUrl}/${id}`);
  }

  create(data: FacturaRequest): Observable<Factura> {
    return this.http.post<Factura>(this.apiUrl, data);
  }

  update(id: number, data: FacturaRequest): Observable<Factura> {
    return this.http.put<Factura>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  downloadPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/pdf`, { responseType: 'blob' });
  }

  enviarPorEmail(id: number, email?: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/enviar-email`, email ? { email } : {});
  }

  /** Recordatorio de cobro al cliente (vencimiento en ≤15 días o vencida). El envío usa el proveedor configurado en la empresa (correo de la aplicación por defecto). */
  enviarRecordatorioCliente(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/recordatorio/cobro`, null);
  }

  registrarCobro(id: number, body: FacturaCobroRequest): Observable<Factura> {
    return this.http.post<Factura>(`${this.apiUrl}/${id}/cobros`, body);
  }

  generarEnlacePago(id: number): Observable<Factura> {
    return this.http.post<Factura>(`${this.apiUrl}/${id}/payment-link`, {});
  }
}
