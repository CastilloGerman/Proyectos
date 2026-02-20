package com.appgestion.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaterialRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200)
        String nombre,

        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0", message = "El precio debe ser mayor o igual a 0")
        Double precioUnitario,

        @Size(max = 50)
        String unidadMedida
) {}
