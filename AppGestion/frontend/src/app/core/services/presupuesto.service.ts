import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Presupuesto, PresupuestoRequest } from '../models/presupuesto.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PresupuestoService {
  private readonly apiUrl = `${environment.apiUrl}/presupuestos`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Presupuesto[]> {
    return this.http.get<Presupuesto[]>(this.apiUrl);
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
}
