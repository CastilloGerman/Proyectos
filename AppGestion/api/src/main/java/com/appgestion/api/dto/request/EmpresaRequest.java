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
         * Base64 de imagen firma (PNG/JPEG). null = no cambiar; cadena vacía = eliminar firma.
         */
        String firmaImagenBase64,
        /**
         * Base64 de logo cabecera (PNG/JPEG). null = no cambiar; cadena vacía = eliminar logo.
         */
        String logoImagenBase64
) {}
