package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EstadoPresupuestoRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado
) {
}
