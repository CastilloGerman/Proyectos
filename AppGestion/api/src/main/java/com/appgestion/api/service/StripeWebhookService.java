package com.appgestion.api.service;

import com.appgestion.api.service.stripe.StripeWebhookProcessingResult;

/**
 * Procesa webhooks de Stripe (firma, idempotencia, despacho).
 */
public interface StripeWebhookService {

    StripeWebhookProcessingResult processWebhook(String payload, String signature);
}
