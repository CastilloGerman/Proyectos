package com.appgestion.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 256, message = "Credenciales inválidas")
        String password,

        /** Opcional: código TOTP de 6 dígitos si el usuario tiene 2FA activo. */
        String totpCode,

        @Valid DeviceClientInfoRequest clientInfo
) {}
