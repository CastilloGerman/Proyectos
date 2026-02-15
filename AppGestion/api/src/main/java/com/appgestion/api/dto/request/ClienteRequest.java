package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200)
        String nombre,

        @Size(max = 50)
        String telefono,

        @Size(max = 150)
        String email,

        @Size(max = 255)
        String direccion,

        @Size(max = 50)
        String dni
) {}
