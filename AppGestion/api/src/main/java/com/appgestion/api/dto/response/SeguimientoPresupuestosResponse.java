package com.appgestion.api.dto.response;

public record SeguimientoPresupuestosResponse(
        boolean seguimientoActivo,
        int seguimientoDiasEspera,
        int seguimientoMaxAvisos,
        boolean seguimientoEmailResumen,
        boolean permitirRespuestaCliente
) {}
