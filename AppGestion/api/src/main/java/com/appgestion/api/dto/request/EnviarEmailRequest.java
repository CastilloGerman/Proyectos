package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * Opcional: permite enviar a un email distinto al del cliente.
 * Si no se envía, se usa el email del cliente.
 */
public record EnviarEmailRequest(
        @Email(message = "El email de destino no es válido")
        @Size(max = 254)
        String email,
        @Size(max = 200, message = "El asunto no puede superar 200 caracteres")
        @Pattern(regexp = "[^\\r\\n]*", message = "El asunto no puede contener saltos de línea")
        String asunto,
        @Size(max = 5000, message = "El mensaje no puede superar 5000 caracteres")
        String mensaje
) {
    public EnviarEmailRequest {
        if (email != null && email.isBlank()) {
            email = null;
        }
    }
}
