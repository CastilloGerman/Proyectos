package com.appgestion.api.dto.response;

public record MaterialResponse(
        Long id,
        String nombre,
        Double precioUnitario,
        String unidadMedida
) {}
