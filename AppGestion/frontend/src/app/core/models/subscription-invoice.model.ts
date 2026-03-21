/** Factura emitida por Stripe por la suscripción al software (no facturas a clientes). */
export interface SubscriptionInvoice {
  id: string;
  number: string | null;
  status: string;
  amountDueCents: number;
  amountPaidCents: number;
  currency: string;
  createdUnix: number;
  invoicePdfUrl: string | null;
  hostedInvoiceUrl: string | null;
}
