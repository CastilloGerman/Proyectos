import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SubscriptionInvoice } from '../models/subscription-invoice.model';

/** Igual que en la API: MONTHLY o YEARLY (JSON en mayúsculas). */
export type CheckoutBillingPeriod = 'MONTHLY' | 'YEARLY';

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

  /** Sin periodo explicito: facturación mensual (equivale al cuerpo vacío `{}`). */
  createCheckoutSession(period?: CheckoutBillingPeriod): Observable<{ checkoutUrl: string }> {
    const body = period === undefined || period === 'MONTHLY' ? {} : { billingPeriod: period };
    return this.http.post<{ checkoutUrl: string }>(`${this.apiUrl}/checkout`, body);
  }

  createPortalSession(): Observable<{ portalUrl: string }> {
    return this.http.post<{ portalUrl: string }>(`${this.apiUrl}/portal`, {});
  }
}
