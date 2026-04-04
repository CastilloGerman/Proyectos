package com.appgestion.api.service.stripe;

/**
 * Resultado del procesamiento del webhook (el controlador traduce a HTTP).
 */
public record StripeWebhookProcessingResult(boolean signatureInvalid) {

    public static StripeWebhookProcessingResult ok() {
        return new StripeWebhookProcessingResult(false);
    }

    public static StripeWebhookProcessingResult badSignature() {
        return new StripeWebhookProcessingResult(true);
    }
}
