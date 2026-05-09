/** Respuesta de GET /subscription/details */
export interface SubscriptionDetails {
  subscriptionStatus: string | null;
  stripePriceId: string | null;
  billingInterval: 'MONTHLY' | 'YEARLY' | 'UNKNOWN';
  cancelAtPeriodEnd: boolean;
  requiresPaymentAction: boolean;
  currentPeriodEnd: string | null;
  displayMonthlyPriceEur: number;
  displayYearlyPriceEur: number;
  yearlySavingsPercentRounded: number;
}
