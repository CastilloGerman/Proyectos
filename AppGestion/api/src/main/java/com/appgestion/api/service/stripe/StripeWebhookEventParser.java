package com.appgestion.api.service.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;

/**
 * Parsea y verifica el payload del webhook de Stripe (sustituye llamadas estáticas para tests).
 */
public interface StripeWebhookEventParser {

    Event parse(String payload, String signature, String webhookSecret) throws SignatureVerificationException;
}
