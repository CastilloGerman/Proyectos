import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly apiUrl = `${environment.apiUrl}/subscription`;

  constructor(private http: HttpClient) {}

  createCheckoutSession(): Observable<{ checkoutUrl: string }> {
    return this.http.post<{ checkoutUrl: string }>(`${this.apiUrl}/checkout`, {});
  }

  createPortalSession(): Observable<{ portalUrl: string }> {
    return this.http.post<{ portalUrl: string }>(`${this.apiUrl}/portal`, {});
  }
}
