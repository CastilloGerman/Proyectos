package com.appgestion.api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

/**
 * Asigna {@link Stripe#apiKey} al arrancar. En el perfil {@code prod} la inicialización perezosa está activa;
 * sin {@link Lazy @Lazy(false)} este bean no se crea (nadie lo inyectaba) y el checkout falla con
 * {@code AuthenticationException: No API key provided} aunque {@code STRIPE_SECRET_KEY} esté en el entorno.
 */
@Configuration
@Lazy(false)
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

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
        applyApiKey();
    }

    /**
     * Garantiza la clave en la llamada, por si este bean se crea después del arranque.
     * No registra el valor de la clave.
     */
    public void ensureConfigured() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank() || looksLikePlaceholder(stripeSecretKey)) {
            throw new IllegalStateException(
                    "Falta STRIPE_SECRET_KEY en la configuración del servidor.");
        }
        applyApiKey();
    }

    private void applyApiKey() {
        String key = stripeSecretKey != null ? stripeSecretKey : "";
        if (key.equals(Stripe.apiKey)) {
            return;
        }
        Stripe.apiKey = key;
        if (!key.isBlank() && !looksLikePlaceholder(key)) {
            log.info("Stripe: clave API aplicada (modo {}).", stripeKeyMode(key));
        }
    }

    private static String stripeKeyMode(String key) {
        if (key.startsWith("sk_live_")) {
            return "live";
        }
        if (key.startsWith("sk_test_")) {
            return "test";
        }
        return "desconocido";
    }

    private static boolean looksLikePlaceholder(String value) {
        String v = value.toLowerCase();
        return v.contains("xxx") || v.contains("placeholder") || v.contains("changeme");
    }
}
