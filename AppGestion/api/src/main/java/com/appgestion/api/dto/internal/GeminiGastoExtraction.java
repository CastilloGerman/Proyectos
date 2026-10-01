package com.appgestion.api.dto.internal;

import java.math.BigDecimal;
import java.util.List;

/** Forma intermedia de la salida del modelo. Nunca se devuelve directamente al cliente. */
public record GeminiGastoExtraction(
        String proveedor,
        String concepto,
        String fecha,
        BigDecimal baseImponible,
        BigDecimal tipoIva,
        String categoria,
        BigDecimal total,
        List<BigDecimal> tiposIvaDetectados,
        List<String> camposDudosos,
        boolean tieneNif,
        boolean esDocumentoValido
) {}
