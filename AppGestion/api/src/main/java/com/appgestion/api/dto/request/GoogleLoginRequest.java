package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank(message = "El token de Google es obligatorio")
        String idToken
) {}
