package com.appgestion.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PresupuestoItemRequest(
        Long materialId,
        String tareaManual,
        @NotNull @DecimalMin("0.0001")
        Double cantidad,
        @NotNull @DecimalMin("0")
        Double precioUnitario,
        Boolean aplicaIva,
        Double descuentoPorcentaje,
        Double descuentoFijo
) {
    public PresupuestoItemRequest {
        if (aplicaIva == null) aplicaIva = true;
        if (descuentoPorcentaje == null) descuentoPorcentaje = 0.0;
        if (descuentoFijo == null) descuentoFijo = 0.0;
    }
}
