package com.appgestion.api.service.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;

/**
 * Obtiene suscripciones desde la API de Stripe (sustituye {@code Subscription.retrieve} para tests).
 */
public interface StripeSubscriptionFetcher {

    Subscription fetch(String subscriptionId) throws StripeException;
}
