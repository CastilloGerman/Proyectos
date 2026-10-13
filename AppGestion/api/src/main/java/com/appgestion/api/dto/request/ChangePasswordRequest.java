package com.appgestion.api.dto.request;

import com.appgestion.api.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Indica tu contraseña actual")
        @Size(max = 128, message = "La contraseña actual es demasiado larga")
        String currentPassword,

        @NotBlank(message = "La nueva contraseña es obligatoria")
        @StrongPassword
        String newPassword
) {}
