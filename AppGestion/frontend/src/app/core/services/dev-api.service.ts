import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface GrantPremiumDevResponse {
  ok: boolean;
  message?: string;
}

/**
 * Endpoints solo para entornos de desarrollo / local (p. ej. activar premium sin Stripe).
 */
@Injectable({ providedIn: 'root' })
export class DevApiService {
  private readonly http = inject(HttpClient);

  grantPremium(): Observable<GrantPremiumDevResponse> {
    return this.http.post<GrantPremiumDevResponse>(`${environment.apiUrl}/dev/grant-premium`, {});
  }
}
