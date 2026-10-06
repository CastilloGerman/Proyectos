package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PresupuestoIaRequest(
        @NotBlank(message = "Describe el trabajo que hay que presupuestar")
        @Size(max = 8000, message = "La descripción no puede superar los 8000 caracteres")
        String texto,
        Long clienteId
) {}
