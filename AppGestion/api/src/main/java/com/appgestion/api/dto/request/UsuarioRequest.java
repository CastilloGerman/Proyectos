package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        @Size(max = 150)
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100)
        String password,

        @Size(max = 50)
        String rol
) {}
