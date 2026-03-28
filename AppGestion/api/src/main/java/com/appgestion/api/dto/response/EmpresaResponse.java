package com.appgestion.api.dto.response;

import java.time.Instant;
import java.util.List;

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
        String rubroAutonomoCodigo,
        Boolean recordatorioClienteActivo,
        /** Días tras el vencimiento en los que enviar (solo 7, 15 y 30). */
        List<Integer> recordatorioClienteDias,
        String emailProvider,
        String oauthProvider,
        Boolean oauthConnected,
        Instant oauthConnectedAt,
        String oauthOnFailure,
        String systemFromOverride
) {}
