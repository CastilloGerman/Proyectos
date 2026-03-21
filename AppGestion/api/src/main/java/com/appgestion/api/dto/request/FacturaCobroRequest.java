package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record FacturaCobroRequest(
        @NotNull @Positive Double importe,
        LocalDate fecha,
        String metodo,
        String notas
) {
    public FacturaCobroRequest {
        if (fecha == null) fecha = LocalDate.now();
    }
}
