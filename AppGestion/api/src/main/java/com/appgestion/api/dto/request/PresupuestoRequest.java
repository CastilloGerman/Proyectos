package com.appgestion.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PresupuestoRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        @NotEmpty(message = "Debe incluir al menos un ítem")
        @Valid
        List<PresupuestoItemRequest> items,

        Boolean ivaHabilitado,
        String estado,
        Double descuentoGlobalPorcentaje,
        Double descuentoGlobalFijo,
        Boolean descuentoAntesIva
) {
    public PresupuestoRequest {
        if (ivaHabilitado == null) ivaHabilitado = true;
        if (estado == null || estado.isBlank()) estado = "Pendiente";
        if (descuentoGlobalPorcentaje == null) descuentoGlobalPorcentaje = 0.0;
        if (descuentoGlobalFijo == null) descuentoGlobalFijo = 0.0;
        if (descuentoAntesIva == null) descuentoAntesIva = true;
    }
}
