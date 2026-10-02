package com.appgestion.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PresupuestoResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        /** PROVISIONAL o COMPLETO; null si el cliente no está cargado. */
        String clienteEstado,
        String clienteEmail,
        LocalDateTime fechaCreacion,
        Double subtotal,
        Double iva,
        Double total,
        Boolean ivaHabilitado,
        String estado,
        Double descuentoGlobalPorcentaje,
        Double descuentoGlobalFijo,
        Boolean descuentoAntesIva,
        List<PresupuestoItemResponse> items,
        List<String> condicionesActivas,
        String notaAdicional,
        Boolean tieneAnticipo,
        java.math.BigDecimal importeAnticipo,
        Boolean anticipoFacturado,
        LocalDate fechaAnticipo,
        /** Id de factura de venta principal (NORMAL o FINAL_CON_ANTICIPO), si existe. */
        Long facturaId
) {}
