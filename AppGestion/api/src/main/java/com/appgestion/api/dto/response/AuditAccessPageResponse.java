package com.appgestion.api.dto.response;

import java.util.List;

public record AuditAccessPageResponse(
        List<AuditAccessEventResponse> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        long failedCount24h,
        long sensitiveCount24h
) {}
