package com.appgestion.api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class StripeConfig {

    private final Environment environment;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${stripe.price-id-monthly:}")
    private String priceIdMonthly;

    @Value("${stripe.price-id-yearly:}")
    private String priceIdYearly;

    @Value("${stripe.success-url:}")
    private String successUrl;

    @Value("${stripe.cancel-url:}")
    private String cancelUrl;

    @Value("${stripe.subscription-cancel-url:}")
    private String subscriptionCancelUrl;

    public StripeConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        if (environment.matchesProfiles("prod")) {
            if (stripeSecretKey == null || stripeSecretKey.isBlank()
                    || looksLikePlaceholder(stripeSecretKey)) {
                throw new IllegalStateException(
                        "Stripe: STRIPE_SECRET_KEY debe estar definida en producción (sin valores tipo sk_xxx de ejemplo).");
            }
            if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()
                    || looksLikePlaceholder(stripeWebhookSecret)) {
                throw new IllegalStateException(
                        "Stripe: STRIPE_WEBHOOK_SECRET debe estar definida en producción.");
            }
            if (priceIdMonthly == null || priceIdMonthly.isBlank()) {
                throw new IllegalStateException(
                        "Stripe: STRIPE_PRICE_MONTHLY debe estar definida en producción (ID price_… del producto/plan).");
            }
            if (priceIdYearly == null || priceIdYearly.isBlank()) {
                throw new IllegalStateException(
                        "Stripe: STRIPE_PRICE_YEARLY debe estar definida en producción (ID price_… anual recurrente).");
            }
            if (successUrl == null || successUrl.isBlank()) {
                throw new IllegalStateException(
                        "Stripe: STRIPE_SUCCESS_URL debe estar definida en producción.");
            }
            if (cancelUrl == null || cancelUrl.isBlank()) {
                throw new IllegalStateException(
                        "Stripe: STRIPE_CANCEL_URL debe estar definida en producción.");
            }
            if (subscriptionCancelUrl == null || subscriptionCancelUrl.isBlank()) {
                throw new IllegalStateException(
                        "Stripe: STRIPE_SUBSCRIPTION_CANCEL_URL (o stripe.subscription-cancel-url) debe estar definida en producción.");
            }
        }
        Stripe.apiKey = stripeSecretKey != null ? stripeSecretKey : "";
    }

    private static boolean looksLikePlaceholder(String value) {
        String v = value.toLowerCase();
        return v.contains("xxx") || v.contains("placeholder") || v.contains("changeme");
    }
}
