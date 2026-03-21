package com.appgestion.api.dto.response;

import com.appgestion.api.domain.enums.NotificacionSeveridad;
import com.appgestion.api.domain.enums.NotificacionTipo;

import java.time.Instant;

public record NotificacionResponse(
        Long id,
        NotificacionTipo tipo,
        NotificacionSeveridad severidad,
        String titulo,
        String resumen,
        boolean leida,
        String actionPath,
        Instant createdAt
) {}
