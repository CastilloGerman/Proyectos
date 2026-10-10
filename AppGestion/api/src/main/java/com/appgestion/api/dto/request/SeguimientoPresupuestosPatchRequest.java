package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SeguimientoPresupuestosPatchRequest(
        Boolean seguimientoActivo,
        @Min(1) @Max(30) Integer seguimientoDiasEspera,
        @Min(1) @Max(5) Integer seguimientoMaxAvisos,
        Boolean seguimientoEmailResumen,
        Boolean permitirRespuestaCliente
) {}
