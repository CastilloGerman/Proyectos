import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Empresa } from '../models/empresa.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private readonly apiUrl = `${environment.apiUrl}/config`;

  constructor(private http: HttpClient) {}

  getEmpresa(): Observable<Empresa> {
    return this.http.get<Empresa>(`${this.apiUrl}/empresa`);
  }

  saveEmpresa(data: Partial<Empresa>): Observable<Empresa> {
    return this.http.put<Empresa>(`${this.apiUrl}/empresa`, data);
  }
}
