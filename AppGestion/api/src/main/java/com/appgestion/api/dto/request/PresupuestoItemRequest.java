package com.appgestion.api.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * SEC-16: todos los importes y porcentajes están acotados. Un descuento negativo (subida encubierta de precio) o
 * superior al 100 % ya no se acepta, y los textos libres tienen tope de longitud.
 */
public record PresupuestoItemRequest(
        Long materialId,
        @Size(max = 500, message = "La descripción de la línea no puede superar 500 caracteres")
        String tareaManual,
        @NotNull @DecimalMin("0.0001") @DecimalMax(value = "1000000", message = "La cantidad es demasiado grande")
        Double cantidad,
        @NotNull @DecimalMin("0") @DecimalMax(value = "100000000", message = "El precio unitario es demasiado grande")
        Double precioUnitario,
        Boolean aplicaIva,
        @DecimalMin(value = "0", message = "El descuento no puede ser negativo")
        @DecimalMax(value = "100", message = "El descuento no puede superar el 100 %")
        Double descuentoPorcentaje,
        @DecimalMin(value = "0", message = "El descuento no puede ser negativo")
        @DecimalMax(value = "100000000", message = "El descuento es demasiado grande")
        Double descuentoFijo,
        Boolean visiblePdf
) {
    public PresupuestoItemRequest {
        if (aplicaIva == null) aplicaIva = true;
        if (descuentoPorcentaje == null) descuentoPorcentaje = 0.0;
        if (descuentoFijo == null) descuentoFijo = 0.0;
        if (visiblePdf == null) visiblePdf = true;
    }
}
