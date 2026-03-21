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

  /** IBAN, Bizum y valores por defecto de factura (PATCH). */
  patchMetodosCobro(body: MetodosCobroPayload): Observable<Empresa> {
    return this.http.patch<Empresa>(`${this.apiUrl}/empresa/metodos-cobro`, body);
  }

  patchDatosFiscales(body: DatosFiscalesPayload): Observable<Empresa> {
    return this.http.patch<Empresa>(`${this.apiUrl}/empresa/datos-fiscales`, body);
  }

  /** Notas al pie de PDFs (presupuesto / factura). */
  patchPlantillasPdf(body: PlantillasPdfPayload): Observable<Empresa> {
    return this.http.patch<Empresa>(`${this.apiUrl}/empresa/plantillas-pdf`, body);
  }
}

export interface MetodosCobroPayload {
  defaultMetodoPago: string;
  defaultCondicionesPago: string;
  ibanCuenta: string;
  bizumTelefono: string;
}

export interface DatosFiscalesPayload {
  regimenIvaPrincipal: string;
  descripcionActividad: string;
  nifIntracomunitario: string;
  epigrafeIae: string;
}

export interface PlantillasPdfPayload {
  notasPiePresupuesto: string | null;
  notasPieFactura: string | null;
}
