package com.appgestion.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record FacturaRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        Long presupuestoId,

        @NotEmpty(message = "Debe incluir al menos un ítem")
        @Valid
        List<FacturaItemRequest> items,

        String numeroFactura,
        LocalDate fechaVencimiento,
        String metodoPago,
        String estadoPago,
        String notas,
        Boolean ivaHabilitado
) {
    public FacturaRequest {
        if (metodoPago == null || metodoPago.isBlank()) metodoPago = "Transferencia";
        if (estadoPago == null || estadoPago.isBlank()) estadoPago = "No Pagada";
        if (ivaHabilitado == null) ivaHabilitado = true;
    }
}
