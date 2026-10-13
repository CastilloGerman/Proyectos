package com.appgestion.api.dto.response;

/**
 * Respuesta de {@code POST /auth/register}. No incluye JWT: la cuenta debe verificarse por email antes de iniciar sesión.
 */
public record RegisterResponse(String email, boolean emailVerificationRequired) {
}
