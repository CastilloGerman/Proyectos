package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpDisableRequest(
        @NotBlank(message = "La contraseña es obligatoria")
        String currentPassword,

        @NotBlank(message = "El código de verificación es obligatorio")
        @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
        String totpCode
) {}
