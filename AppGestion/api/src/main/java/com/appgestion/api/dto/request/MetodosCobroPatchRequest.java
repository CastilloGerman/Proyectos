package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Actualización parcial de datos de cobro a clientes (facturas).
 */
public record MetodosCobroPatchRequest(
        @Size(max = 50) String defaultMetodoPago,
        @Size(max = 200) String defaultCondicionesPago,
        @Size(max = 34) String ibanCuenta,
        @Size(max = 20) String bizumTelefono
) {}
