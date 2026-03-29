package com.appgestion.api.dto.response;

import java.math.BigDecimal;

/**
 * Vista previa de importes para el flujo de anticipo (mismos cálculos que al emitir facturas).
 */
public record AnticipoResumenDTO(
        BigDecimal totalPresupuesto,
        BigDecimal importeAnticipo,
        BigDecimal baseAnticipo,
        BigDecimal ivaAnticipo,
        BigDecimal importePendiente,
        BigDecimal basePendiente,
        BigDecimal ivaPendiente,
        boolean anticipoYaFacturado,
        boolean tieneAnticipoRegistrado
) {}
