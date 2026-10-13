package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Cambio de estado de pago desde el listado. No incluye líneas ni importes de la factura.
 */
public record EstadoPagoFacturaRequest(
        @NotBlank String estadoPago,
        LocalDate fechaCobro,
        Double montoCobrado
) {
}
