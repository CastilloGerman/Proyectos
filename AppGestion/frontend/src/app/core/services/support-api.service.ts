import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface SupportContactResponse {
  message?: string;
}

/**
 * Formulario de contacto con soporte (multipart); alineado con {@code POST /auth/support/contact}.
 */
@Injectable({ providedIn: 'root' })
export class SupportApiService {
  private readonly http = inject(HttpClient);

  contact(formData: FormData): Observable<SupportContactResponse> {
    return this.http.post<SupportContactResponse>(`${environment.apiUrl}/auth/support/contact`, formData);
  }
}
