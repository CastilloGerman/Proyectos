package com.appgestion.api.dto.request;

import com.appgestion.api.validation.StrongPassword;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        @Size(max = 254, message = "Email inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @StrongPassword
        String password,

        @Size(max = 256, message = "Enlace de invitación inválido")
        String referralToken,

        @Valid DeviceClientInfoRequest clientInfo
) {
    // SEC-01: el rol NUNCA se acepta del cliente. Cualquier campo "rol" enviado en el JSON se ignora
    // (ignoreUnknown) y el servidor asigna siempre el rol por defecto.
}
