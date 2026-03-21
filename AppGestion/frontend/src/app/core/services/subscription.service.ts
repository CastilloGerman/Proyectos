import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SubscriptionInvoice } from '../models/subscription-invoice.model';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {
  private readonly apiUrl = `${environment.apiUrl}/subscription`;

  constructor(private http: HttpClient) {}

  /** Facturas de la suscripción (Stripe) del usuario autenticado. */
  getSubscriptionInvoices(limit = 24): Observable<SubscriptionInvoice[]> {
    return this.http.get<SubscriptionInvoice[]>(`${this.apiUrl}/invoices`, {
      params: { limit: String(limit) },
    });
  }

  createCheckoutSession(): Observable<{ checkoutUrl: string }> {
    return this.http.post<{ checkoutUrl: string }>(`${this.apiUrl}/checkout`, {});
  }

  createPortalSession(): Observable<{ portalUrl: string }> {
    return this.http.post<{ portalUrl: string }>(`${this.apiUrl}/portal`, {});
  }
}
