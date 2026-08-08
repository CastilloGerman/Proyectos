package com.appgestion.api.dto.response;

import com.appgestion.api.domain.enums.GastoCategoria;

import java.time.LocalDate;

public record GastoResponse(
        Long id,
        String proveedor,
        String concepto,
        LocalDate fecha,
        Double baseImponible,
        Double tipoIva,
        Double cuotaIva,
        GastoCategoria categoria
) {}
