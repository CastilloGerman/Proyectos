package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registro del cobro de anticipo antes de emitir la factura de anticipo.
 */
public record AnticipoRegistroRequest(
        @NotNull @Positive BigDecimal importeAnticipo,
        @NotNull LocalDate fechaAnticipo
) {}
