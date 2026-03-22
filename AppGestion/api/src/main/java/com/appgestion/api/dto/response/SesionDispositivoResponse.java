package com.appgestion.api.dto.response;

import java.time.Instant;

public record SesionDispositivoResponse(
        String id,
        Instant createdAt,
        Instant lastActivityAt,
        Instant expiresAt,
        String ipAddress,
        String browser,
        String osName,
        String deviceType,
        String clientLabel,
        String userAgentPreview,
        boolean currentSession
) {}
