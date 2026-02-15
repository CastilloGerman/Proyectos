package com.appgestion.api.dto.response;

import java.time.Instant;

public record AuthResponse(
        String token,
        String type,
        String email,
        String rol,
        Instant expiresAt
) {
    public static AuthResponse of(String token, String email, String rol, Instant expiresAt) {
        return new AuthResponse(token, "Bearer", email, rol, expiresAt);
    }
}
