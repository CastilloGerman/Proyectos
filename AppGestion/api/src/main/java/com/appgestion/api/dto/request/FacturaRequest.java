package com.appgestion.api.dto.request;

import com.appgestion.api.constant.FacturaEstadoPago;
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

        @NotNull(message = "La fecha de expedición es obligatoria")
        LocalDate fechaExpedicion,
        LocalDate fechaOperacion,
        LocalDate fechaVencimiento,
        String regimenFiscal,
        String condicionesPago,
        String metodoPago,
        String estadoPago,
        Double montoCobrado,
        String notas,
        Boolean ivaHabilitado
) {
    public FacturaRequest {
        if (metodoPago == null || metodoPago.isBlank()) metodoPago = "Transferencia";
        if (estadoPago == null || estadoPago.isBlank()) estadoPago = FacturaEstadoPago.NO_PAGADA;
        if (ivaHabilitado == null) ivaHabilitado = true;
    }
}
