package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresupuestoEstadoRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado
) {
}
