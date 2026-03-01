package com.appgestion.api.domain.enums;

/**
 * Estados de suscripción del usuario.
 * TRIAL_ACTIVE y ACTIVE permiten lectura y escritura.
 * TRIAL_EXPIRED, PAST_DUE y CANCELED solo permiten lectura.
 */
public enum SubscriptionStatus {
    /** Trial de 14 días activo - acceso completo */
    TRIAL_ACTIVE,
    /** Trial expirado - solo lectura */
    TRIAL_EXPIRED,
    /** Suscripción pagada activa - acceso completo */
    ACTIVE,
    /** Pago fallido - solo lectura */
    PAST_DUE,
    /** Suscripción cancelada - solo lectura */
    CANCELED
}
