package com.appgestion.api.dto.response;

import java.math.BigDecimal;

/**
 * Una fila del desglose de IVA repercutido. {@code tipoIva} 0 junta exento y gravado al 0 %.
 */
public record DesgloseIvaFilaResponse(
        int tipoIva,
        String etiqueta,
        BigDecimal base,
        BigDecimal cuota
) {}
