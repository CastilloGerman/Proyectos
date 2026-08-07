package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PresupuestoEstadoPatchRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado
) {}
