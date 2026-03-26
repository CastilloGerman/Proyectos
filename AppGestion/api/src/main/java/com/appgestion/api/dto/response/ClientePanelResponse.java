package com.appgestion.api.dto.response;

import java.util.List;

public record ClientePanelResponse(
        ClienteResponse cliente,
        double totalPendienteCobro,
        int facturasConPendiente,
        List<ClienteFacturaResumen> facturas,
        List<ClientePresupuestoResumen> presupuestos,
        List<ClientePresupuestoResumen> presupuestosActivos,
        List<ClienteHistorialItem> historial
) {}
