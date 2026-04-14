package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200)
        String nombre,

        @Size(max = 50)
        String telefono,

        @Email(message = "El email no tiene formato válido")
        @Size(max = 150)
        String email,

        @Size(max = 255)
        String direccion,

        @Size(max = 10)
        @Pattern(
                regexp = "^[0-9]{5}$",
                message = "El código postal debe tener 5 dígitos")
        String codigoPostal,

        @Size(max = 100)
        String provincia,

        @Size(max = 100)
        String pais,

        @Size(max = 50)
        String dni
) {}
