package com.appgestion.api.dto.response;

import java.time.LocalDateTime;

public record ClientePresupuestoResumen(
        Long id,
        LocalDateTime fechaCreacion,
        Double total,
        String estado,
        Long facturaId,
        boolean activo
) {}
