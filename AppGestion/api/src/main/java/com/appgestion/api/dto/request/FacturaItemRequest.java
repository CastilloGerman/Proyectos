package com.appgestion.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** SEC-16: importes acotados y textos con tope de longitud. */
public record FacturaItemRequest(
        Long materialId,
        @Size(max = 500, message = "La descripción de la línea no puede superar 500 caracteres")
        String tareaManual,
        @NotNull @DecimalMin("0.0001") @DecimalMax(value = "1000000", message = "La cantidad es demasiado grande")
        Double cantidad,
        @NotNull @DecimalMin("0") @DecimalMax(value = "100000000", message = "El precio unitario es demasiado grande")
        Double precioUnitario,
        Boolean aplicaIva,
        @Min(value = 0, message = "Tipo de IVA no válido") @Max(value = 100, message = "Tipo de IVA no válido")
        Integer tipoIva
) {
    public FacturaItemRequest {
        if (aplicaIva == null) aplicaIva = true;
    }
}
