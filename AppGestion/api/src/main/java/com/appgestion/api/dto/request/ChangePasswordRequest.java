package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Indica tu contraseña actual")
        String currentPassword,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @Size(min = 8, max = 128, message = "La nueva contraseña debe tener entre 8 y 128 caracteres")
        String newPassword
) {}
