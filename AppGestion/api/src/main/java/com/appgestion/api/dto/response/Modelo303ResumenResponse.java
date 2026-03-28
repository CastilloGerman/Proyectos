package com.appgestion.api.dto.response;

import com.appgestion.api.domain.enums.FiscalCriterioImputacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Resumen orientativo para ayudar al autónomo con datos tipo Modelo 303.
 * No sustituye la presentación oficial en la AEAT.
 */
public record Modelo303ResumenResponse(
        int anio,
        int trimestre,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        FiscalCriterioImputacion criterio,
        boolean soloFacturasPagadas,
        BigDecimal baseImponibleTotal,
        BigDecimal ivaRepercutido,
        BigDecimal ivaSoportado,
        boolean ivaSoportadoCalculado,
        String ivaSoportadoNota,
        BigDecimal resultadoIva,
        boolean resultadoEsIngreso,
        long numeroFacturas,
        List<String> advertencias,
        String avisoLegal
) {}
