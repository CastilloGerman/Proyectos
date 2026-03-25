package com.appgestion.api.dto.response;

public record EmpresaResponse(
        Long id,
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
        Boolean mailConfigurado,
        Boolean tieneFirma,
        /** Base64 para previsualizar en configuración (solo si hay firma). */
        String firmaImagenBase64,
        Boolean tieneLogo,
        /** Base64 para previsualizar logo en configuración. */
        String logoImagenBase64,
        String defaultMetodoPago,
        String defaultCondicionesPago,
        String ibanCuenta,
        String bizumTelefono,
        String regimenIvaPrincipal,
        String descripcionActividadFiscal,
        String nifIntracomunitario,
        String epigrafeIae,
        /** Solo métricas internas; no se usa en PDF. */
        String rubroAutonomoCodigo
) {}
