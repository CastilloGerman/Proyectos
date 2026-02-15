package com.appgestion.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record FacturaItemRequest(
        Long materialId,
        String tareaManual,
        @NotNull @DecimalMin("0.0001")
        Double cantidad,
        @NotNull @DecimalMin("0")
        Double precioUnitario,
        Boolean aplicaIva
) {
    public FacturaItemRequest {
        if (aplicaIva == null) aplicaIva = true;
    }
}
