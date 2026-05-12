package com.appgestion.api.dto.response;

/** Respuesta GET /auth/invite/verify: solo indica si el token sigue siendo válido (no muestra email). */
public record InviteVerifyResponse(boolean valid) {
    public static InviteVerifyResponse invalid() {
        return new InviteVerifyResponse(false);
    }
}
