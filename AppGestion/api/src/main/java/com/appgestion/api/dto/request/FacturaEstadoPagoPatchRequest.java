package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FacturaEstadoPagoPatchRequest(
        @NotBlank String estadoPago,
        Double montoCobrado
) {}
