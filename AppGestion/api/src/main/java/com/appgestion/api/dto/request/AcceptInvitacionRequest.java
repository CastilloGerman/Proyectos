package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitacionRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password,

        DeviceClientInfoRequest clientInfo
) {}
