package com.appgestion.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FacturaCobroResponse(
        Long id,
        Double importe,
        LocalDate fecha,
        String metodo,
        String notas,
        LocalDateTime createdAt
) {}
