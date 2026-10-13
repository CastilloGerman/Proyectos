package com.appgestion.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** SEC-16: importes, porcentajes y tamaños acotados (ver {@link PresupuestoItemRequest}). */
public record PresupuestoRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        @NotEmpty(message = "Debe incluir al menos un ítem")
        @Size(max = 500, message = "Un presupuesto no puede tener más de 500 líneas")
        @Valid
        List<PresupuestoItemRequest> items,

        Boolean ivaHabilitado,
        @Size(max = 30, message = "Estado no válido")
        String estado,
        @DecimalMin(value = "0", message = "El descuento global no puede ser negativo")
        @DecimalMax(value = "100", message = "El descuento global no puede superar el 100 %")
        Double descuentoGlobalPorcentaje,
        @DecimalMin(value = "0", message = "El descuento global no puede ser negativo")
        @DecimalMax(value = "100000000", message = "El descuento global es demasiado grande")
        Double descuentoGlobalFijo,
        Boolean descuentoAntesIva,
        /** Claves del catálogo (servidor); vacío o null en creación usa predeterminadas del perfil si existen. */
        @Size(max = 50, message = "Demasiadas condiciones seleccionadas")
        List<@Size(max = 100, message = "Condición no válida") String> condicionesActivas,
        @Size(max = 5000, message = "La nota adicional no puede superar 5000 caracteres")
        String notaAdicional
) {
    public PresupuestoRequest {
        if (ivaHabilitado == null) ivaHabilitado = true;
        if (estado == null || estado.isBlank()) estado = "Pendiente";
        if (descuentoGlobalPorcentaje == null) descuentoGlobalPorcentaje = 0.0;
        if (descuentoGlobalFijo == null) descuentoGlobalFijo = 0.0;
        if (descuentoAntesIva == null) descuentoAntesIva = true;
    }
}
