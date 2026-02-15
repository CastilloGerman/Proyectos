package com.appgestion.api.dto.response;

public record FacturaItemResponse(
        Long id,
        String descripcion,
        Boolean esTareaManual,
        Double cantidad,
        Double precioUnitario,
        Double subtotal
) {}
