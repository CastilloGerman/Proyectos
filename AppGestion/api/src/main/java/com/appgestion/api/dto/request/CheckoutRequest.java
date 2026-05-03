package com.appgestion.api.dto.request;

import com.appgestion.api.dto.SubscriptionBillingPeriod;

/**
 * Cuerpo opcional de POST {@code /subscription/checkout}.
 * Sin campo {@code billingPeriod}: se usa facturación mensual.
 */
public record CheckoutRequest(SubscriptionBillingPeriod billingPeriod) {

    public static SubscriptionBillingPeriod effectivePeriod(CheckoutRequest body) {
        if (body == null || body.billingPeriod() == null) {
            return SubscriptionBillingPeriod.MONTHLY;
        }
        return body.billingPeriod();
    }
}
