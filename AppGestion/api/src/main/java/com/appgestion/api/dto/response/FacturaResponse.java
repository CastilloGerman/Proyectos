package com.appgestion.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaResponse(
        Long id,
        String numeroFactura,
        Long clienteId,
        String clienteNombre,
        String clienteEmail,
        Long presupuestoId,
        LocalDateTime fechaCreacion,
        LocalDate fechaExpedicion,
        LocalDate fechaOperacion,
        LocalDate fechaVencimiento,
        Double subtotal,
        Double iva,
        Double total,
        Boolean ivaHabilitado,
        String regimenFiscal,
        String condicionesPago,
        String metodoPago,
        String estadoPago,
        String notas,
        List<FacturaItemResponse> items
) {}
