package com.appgestion.api.dto.request;

public record EmpresaRequest(
        String nombre,
        String direccion,
        String codigoPostal,
        String provincia,
        String pais,
        String nif,
        String telefono,
        String email,
        String notasPiePresupuesto,
        String notasPieFactura,
        String mailHost,
        Integer mailPort,
        String mailUsername,
        String mailPassword,
        /**
         * Base64 de logo cabecera (PNG/JPEG). null = no cambiar; cadena vacía = eliminar logo.
         */
        String logoImagenBase64,
        /**
         * Código de rubro (métricas). null = no modificar el valor guardado; "" = borrar.
         */
        String rubroAutonomoCodigo,
        /** system | gmail | outlook | smtp_legacy — null = no cambiar */
        String emailProvider,
        /** system | fail — null = no cambiar */
        String oauthOnFailure,
        /** Remitente From en modo system (override); null = no cambiar */
        String systemFromOverride
) {}
