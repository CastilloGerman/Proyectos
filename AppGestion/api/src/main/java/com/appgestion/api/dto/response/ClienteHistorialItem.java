package com.appgestion.api.dto.response;

import java.time.LocalDateTime;

public record ClienteHistorialItem(
        String tipo,
        Long id,
        LocalDateTime fechaOrden,
        String etiqueta,
        Double importe,
        String estado,
        String subetiqueta
) {}
