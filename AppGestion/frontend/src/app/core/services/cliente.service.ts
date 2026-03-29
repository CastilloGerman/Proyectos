import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Cliente,
  ClienteCompletoRequest,
  ClienteProvisionalRequest,
  ClienteRequest,
} from '../models/cliente.model';
import { ClientePanel } from '../models/cliente-panel.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly apiUrl = `${environment.apiUrl}/clientes`;

  constructor(private http: HttpClient) {}

  getAll(q?: string, estado?: string): Observable<Cliente[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (estado) params = params.set('estado', estado);
    const opts = params.keys().length ? { params } : {};
    return this.http.get<Cliente[]>(this.apiUrl, opts);
  }

  createProvisional(body: ClienteProvisionalRequest): Observable<Cliente> {
    return this.http.post<Cliente>(`${this.apiUrl}/provisional`, body);
  }

  completar(id: number, body: ClienteCompletoRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.apiUrl}/${id}/completar`, body);
  }

  getById(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.apiUrl}/${id}`);
  }

  getPanel(id: number): Observable<ClientePanel> {
    return this.http.get<ClientePanel>(`${this.apiUrl}/${id}/panel`);
  }

  create(data: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>(this.apiUrl, data);
  }

  update(id: number, data: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
