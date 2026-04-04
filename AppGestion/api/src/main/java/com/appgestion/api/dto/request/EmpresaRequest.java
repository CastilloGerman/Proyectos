package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmpresaRequest(
        @NotBlank(message = "El nombre de la empresa es obligatorio")
        @Size(max = 200)
        String nombre,
        @Size(max = 255)
        String direccion,
        @Size(max = 10)
        String codigoPostal,
        @Size(max = 100)
        String provincia,
        @Size(max = 100)
        String pais,
        @Size(max = 20)
        String nif,
        @Size(max = 50)
        String telefono,
        @Email(message = "El email de empresa no es válido")
        @Size(max = 150)
        String email,
        @Size(max = 1000)
        String notasPiePresupuesto,
        @Size(max = 1000)
        String notasPieFactura,
        @Size(max = 100)
        String mailHost,
        @Min(1)
        @Max(65535)
        Integer mailPort,
        @Size(max = 150)
        String mailUsername,
        @Size(max = 255)
        String mailPassword,
        /**
         * Base64 de logo cabecera (PNG/JPEG). null = no cambiar; cadena vacía = eliminar logo.
         */
        @Size(max = 600_000)
        String logoImagenBase64,
        /**
         * Código de rubro (métricas). null = no modificar el valor guardado; "" = borrar.
         */
        @Size(max = 64)
        String rubroAutonomoCodigo,
        /** system | gmail | outlook | smtp_legacy — null = no cambiar */
        @Size(max = 32)
        String emailProvider,
        /** system | fail — null = no cambiar */
        @Size(max = 16)
        String oauthOnFailure,
        /** Remitente From en modo system (override); null = no cambiar */
        @Size(max = 255)
        String systemFromOverride
) {
    public EmpresaRequest {
        if (email != null && email.isBlank()) {
            email = null;
        }
    }
}
