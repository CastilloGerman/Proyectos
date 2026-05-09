package com.appgestion.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista enriquecida de suscripción para el front (pricing, ahorro anual, banderas Stripe).
 */
public record SubscriptionDetailsDto(
        String subscriptionStatus,
        String stripePriceId,
        /** MONTHLY, YEARLY o UNKNOWN si el price no coincide con la config del servidor. */
        String billingInterval,
        boolean cancelAtPeriodEnd,
        boolean requiresPaymentAction,
        LocalDateTime currentPeriodEnd,
        BigDecimal displayMonthlyPriceEur,
        BigDecimal displayYearlyPriceEur,
        int yearlySavingsPercentRounded) {}
