package com.appgestion.api.dto.response;

/**
 * Factura de suscripción (Stripe) mostrada al usuario autónomo.
 */
public record SubscriptionInvoiceDto(
        String id,
        /** Número legible en Stripe (puede ser null en borradores). */
        String number,
        String status,
        long amountDueCents,
        long amountPaidCents,
        String currency,
        /** Epoch seconds (Stripe). */
        long createdUnix,
        String invoicePdfUrl,
        String hostedInvoiceUrl
) {}
