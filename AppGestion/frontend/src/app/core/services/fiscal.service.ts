import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FiscalCriterio, FiscalPlazoActual, Modelo303Resumen, Modelo347Resumen } from '../models/fiscal.model';

@Injectable({ providedIn: 'root' })
export class FiscalService {
  private readonly base = `${environment.apiUrl}/fiscal`;

  constructor(private http: HttpClient) {}

  getModelo303(
    year: number,
    trimestre: number,
    criterio: FiscalCriterio,
    soloPagadas: boolean,
  ): Observable<Modelo303Resumen> {
    let params = new HttpParams()
      .set('year', String(year))
      .set('trimestre', String(trimestre))
      .set('criterio', criterio)
      .set('soloPagadas', String(soloPagadas));
    return this.http.get<Modelo303Resumen>(`${this.base}/modelo303`, { params });
  }

  downloadModelo303Pdf(
    year: number,
    trimestre: number,
    criterio: FiscalCriterio,
    soloPagadas: boolean,
  ): Observable<Blob> {
    let params = new HttpParams()
      .set('year', String(year))
      .set('trimestre', String(trimestre))
      .set('criterio', criterio)
      .set('soloPagadas', String(soloPagadas));
    return this.http.get(`${this.base}/modelo303/pdf`, {
      params,
      responseType: 'blob',
    });
  }

  getModelo347(year: number): Observable<Modelo347Resumen> {
    const params = new HttpParams().set('year', String(year));
    return this.http.get<Modelo347Resumen>(`${this.base}/modelo347`, { params });
  }

  getPlazoActual(): Observable<FiscalPlazoActual> {
    return this.http.get<FiscalPlazoActual>(`${this.base}/plazo-actual`);
  }
}
