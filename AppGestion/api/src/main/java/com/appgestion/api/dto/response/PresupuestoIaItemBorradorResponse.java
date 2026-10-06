package com.appgestion.api.dto.response;

/** Partida editable para revisión. Los campos de creación coinciden con PresupuestoItemRequest. */
public record PresupuestoIaItemBorradorResponse(
        Long materialId,
        String materialNombre,
        String tareaManual,
        Double cantidad,
        Double precioUnitario,
        String unidad,
        Boolean aplicaIva,
        Double descuentoPorcentaje,
        Double descuentoFijo,
        Boolean visiblePdf,
        String confianza,
        boolean faltaPrecio,
        boolean cantidadDudosa
) {}
