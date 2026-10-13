package com.appgestion.api.dto.request;

import com.appgestion.api.constant.FacturaEstadoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** SEC-16: importes acotados y textos con tope de longitud (coherente con las columnas de {@code facturas}). */
public record FacturaRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        Long presupuestoId,

        @NotEmpty(message = "Debe incluir al menos un ítem")
        @Size(max = 500, message = "Una factura no puede tener más de 500 líneas")
        @Valid
        List<FacturaItemRequest> items,

        @Size(max = 50, message = "El número de factura no puede superar 50 caracteres")
        String numeroFactura,

        @NotNull(message = "La fecha de expedición es obligatoria")
        LocalDate fechaExpedicion,
        LocalDate fechaOperacion,
        LocalDate fechaVencimiento,
        @Size(max = 100, message = "El régimen fiscal no puede superar 100 caracteres")
        String regimenFiscal,
        @Size(max = 100, message = "Las condiciones de pago no pueden superar 100 caracteres")
        String condicionesPago,
        @Size(max = 50, message = "El método de pago no puede superar 50 caracteres")
        String metodoPago,
        @Size(max = 50, message = "Estado de pago no válido")
        String estadoPago,
        @DecimalMin(value = "0", message = "El importe cobrado no puede ser negativo")
        @DecimalMax(value = "1000000000", message = "El importe cobrado es demasiado grande")
        Double montoCobrado,
        @Size(max = 1000, message = "Las notas no pueden superar 1000 caracteres")
        String notas,
        Boolean ivaHabilitado
) {
    public FacturaRequest {
        if (metodoPago == null || metodoPago.isBlank()) metodoPago = "Transferencia";
        if (estadoPago == null || estadoPago.isBlank()) estadoPago = FacturaEstadoPago.NO_PAGADA;
        if (ivaHabilitado == null) ivaHabilitado = true;
    }
}
