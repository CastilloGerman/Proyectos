package com.appgestion.api.domain.enums;

/**
 * Estado de suscripción / acceso (app + Stripe Billing).
 * TRIAL_ACTIVE viene del trial de onboarding de la app; TRIALING es trial nativo de Stripe en una suscripción de pago.
 * INCOMPLETE / UNPAID bloquean escritura salvo política explícita.
 */
public enum SubscriptionStatus {
    /** Trial de 14 días activo - acceso completo */
    TRIAL_ACTIVE,
    /** Trial expirado - solo lectura */
    TRIAL_EXPIRED,
    /** Suscripción pagada activa - acceso completo */
    ACTIVE,
    /** Trial de Stripe (primer periodo) - acceso completo */
    TRIALING,
    /** Pago fallido - solo lectura */
    PAST_DUE,
    /** Cobro no completado o SCA pendiente - solo lectura */
    INCOMPLETE,
    /** Tras varios fallos de cobro - solo lectura */
    UNPAID,
    /** Suscripción terminada - solo lectura */
    CANCELED
}
