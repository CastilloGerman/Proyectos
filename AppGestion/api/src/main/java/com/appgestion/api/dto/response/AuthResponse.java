package com.appgestion.api.dto.response;

import com.appgestion.api.domain.enums.SubscriptionStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AuthResponse(
        String token,
        String type,
        String email,
        String rol,
        Instant expiresAt,
        String subscriptionStatus,
        LocalDate trialEndDate,
        boolean canWrite
) {
    public static AuthResponse of(String token, String email, String rol, Instant expiresAt,
                                  SubscriptionStatus subscriptionStatus, LocalDate trialEndDate, boolean canWrite) {
        return new AuthResponse(
                token, "Bearer", email, rol, expiresAt,
                subscriptionStatus != null ? subscriptionStatus.name() : null,
                trialEndDate,
                canWrite
        );
    }
}
