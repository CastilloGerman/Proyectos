package com.appgestion.api.dto.response;

public record PresupuestoItemResponse(
        Long id,
        Long materialId,
        String descripcion,
        Boolean esTareaManual,
        Double cantidad,
        Double precioUnitario,
        Double subtotal,
        Boolean visiblePdf
) {}
