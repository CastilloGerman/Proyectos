package com.appgestion.api.dto.response;

import com.appgestion.api.domain.enums.GastoCategoria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Propuesta sin persistir; el total detectado se usa internamente para validar importes. */
public record GastoBorradorResponse(
        String proveedor,
        String concepto,
        LocalDate fecha,
        BigDecimal baseImponible,
        BigDecimal tipoIva,
        GastoCategoria categoria,
        List<String> camposDudosos,
        boolean tieneNif,
        boolean esDocumentoValido
) {}
