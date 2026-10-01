import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Gasto, GastoBorradorResponse, GastoRequest } from '../models/gasto.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class GastoService {
  private readonly apiUrl = `${environment.apiUrl}/gastos`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Gasto[]> {
    return this.http.get<Gasto[]>(this.apiUrl);
  }

  getById(id: number): Observable<Gasto> {
    return this.http.get<Gasto>(`${this.apiUrl}/${id}`);
  }

  create(data: GastoRequest): Observable<Gasto> {
    return this.http.post<Gasto>(this.apiUrl, data);
  }

  extractDraft(file: File): Observable<GastoBorradorResponse> {
    const formData = new FormData();
    formData.append('archivo', file);
    return this.http.post<GastoBorradorResponse>(`${this.apiUrl}/ia/extraer`, formData);
  }

  update(id: number, data: GastoRequest): Observable<Gasto> {
    return this.http.put<Gasto>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
