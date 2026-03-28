package com.appgestion.api.dto.response;

import java.math.BigDecimal;

/**
 * Cliente con base anual que supera el umbral orientativo del modelo 347 (3.005,06 €).
 */
public record Modelo347ClienteResponse(
        Long clienteId,
        String nombre,
        String dni,
        BigDecimal baseImponibleAnual
) {}
