package com.appgestion.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaResponse(
        Long id,
        String numeroFactura,
        Long clienteId,
        String clienteNombre,
        Long presupuestoId,
        LocalDateTime fechaCreacion,
        LocalDate fechaVencimiento,
        Double subtotal,
        Double iva,
        Double total,
        Boolean ivaHabilitado,
        String metodoPago,
        String estadoPago,
        String notas,
        List<FacturaItemResponse> items
) {}
