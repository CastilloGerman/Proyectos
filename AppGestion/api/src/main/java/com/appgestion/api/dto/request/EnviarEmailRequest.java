package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Opcional: permite enviar a un email distinto al del cliente.
 * Si no se envía, se usa el email del cliente.
 */
public record EnviarEmailRequest(
        @Email(message = "El email de destino no es válido")
        @Size(max = 254)
        String email
) {
    public EnviarEmailRequest {
        if (email != null && email.isBlank()) {
            email = null;
        }
    }
}
