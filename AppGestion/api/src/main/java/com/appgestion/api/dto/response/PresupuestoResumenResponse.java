package com.appgestion.api.dto.response;

public record PresupuestoResumenResponse(
        Long id,
        String clienteNombre,
        String estado
) {
}
