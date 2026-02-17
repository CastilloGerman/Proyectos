import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Factura, FacturaRequest } from '../models/factura.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FacturaService {
  private readonly apiUrl = `${environment.apiUrl}/facturas`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Factura[]> {
    return this.http.get<Factura[]>(this.apiUrl);
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
}
