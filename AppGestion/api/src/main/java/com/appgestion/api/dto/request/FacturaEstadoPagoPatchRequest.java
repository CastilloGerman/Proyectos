package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FacturaEstadoPagoPatchRequest(
        @NotBlank(message = "El estado de pago es obligatorio")
        String estadoPago,
        Double montoCobrado
) {}
