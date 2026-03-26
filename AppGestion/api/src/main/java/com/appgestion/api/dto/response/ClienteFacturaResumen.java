package com.appgestion.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteFacturaResumen(
        Long id,
        String numeroFactura,
        LocalDateTime fechaCreacion,
        LocalDate fechaExpedicion,
        LocalDate fechaVencimiento,
        Double total,
        String estadoPago,
        Double montoCobrado,
        Double pendiente
) {}
