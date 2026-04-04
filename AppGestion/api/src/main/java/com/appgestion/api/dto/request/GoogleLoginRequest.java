package com.appgestion.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "El token de Google es obligatorio")
        String idToken,

        /** Opcional: TOTP si la cuenta tiene 2FA activo. */
        String totpCode,

        @Valid DeviceClientInfoRequest clientInfo
) {}
