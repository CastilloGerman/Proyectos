package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        /** Opcional: código TOTP de 6 dígitos si el usuario tiene 2FA activo. */
        String totpCode
) {}
