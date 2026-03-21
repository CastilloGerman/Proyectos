package com.appgestion.api.dto.response;

/**
 * Respuesta al iniciar el enrolamiento 2FA: URI para QR y clave manual (entrada en la app).
 */
public record TotpSetupStartResponse(
        String otpAuthUrl,
        String secretBase32,
        /** Minutos hasta que caduque el enrolamiento pendiente en servidor. */
        int pendingExpiresInMinutes
) {}
