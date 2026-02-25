package com.appgestion.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PresupuestoResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        String clienteEmail,
        LocalDateTime fechaCreacion,
        Double subtotal,
        Double iva,
        Double total,
        Boolean ivaHabilitado,
        String estado,
        Double descuentoGlobalPorcentaje,
        Double descuentoGlobalFijo,
        Boolean descuentoAntesIva,
        List<PresupuestoItemResponse> items
) {}
