import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Material, MaterialRequest } from '../models/material.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class MaterialService {
  private readonly apiUrl = `${environment.apiUrl}/materiales`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Material[]> {
    return this.http.get<Material[]>(this.apiUrl);
  }

  getById(id: number): Observable<Material> {
    return this.http.get<Material>(`${this.apiUrl}/${id}`);
  }

  create(data: MaterialRequest): Observable<Material> {
    return this.http.post<Material>(this.apiUrl, data);
  }

  update(id: number, data: MaterialRequest): Observable<Material> {
    return this.http.put<Material>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
