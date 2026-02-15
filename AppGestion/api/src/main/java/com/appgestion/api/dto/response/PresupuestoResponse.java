package com.appgestion.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PresupuestoResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        LocalDateTime fechaCreacion,
        Double subtotal,
        Double iva,
        Double total,
        Boolean ivaHabilitado,
        String estado,
        List<PresupuestoItemResponse> items
) {}
