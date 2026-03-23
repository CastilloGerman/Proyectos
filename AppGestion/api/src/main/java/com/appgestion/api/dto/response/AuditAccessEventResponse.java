package com.appgestion.api.dto.response;

import java.time.Instant;

public record AuditAccessEventResponse(
        long id,
        Instant occurredAt,
        Long usuarioId,
        String userEmail,
        String eventType,
        boolean success,
        String failureReason,
        String ipAddress,
        String userAgent,
        String countryCode,
        String sessionId,
        String resourcePath,
        String traceId,
        boolean sensitive,
        String metadataJson
) {}
