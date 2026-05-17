package com.appgestion.api.service.stripe;

/**
 * Resultado del procesamiento del webhook (el controlador traduce a HTTP).
 */
public record StripeWebhookProcessingResult(boolean signatureInvalid, boolean processingFailed) {

    public StripeWebhookProcessingResult(boolean signatureInvalid) {
        this(signatureInvalid, false);
    }

    public static StripeWebhookProcessingResult ok() {
        return new StripeWebhookProcessingResult(false, false);
    }

    public static StripeWebhookProcessingResult badSignature() {
        return new StripeWebhookProcessingResult(true, false);
    }

    public static StripeWebhookProcessingResult processingFailed() {
        return new StripeWebhookProcessingResult(false, true);
    }
}
