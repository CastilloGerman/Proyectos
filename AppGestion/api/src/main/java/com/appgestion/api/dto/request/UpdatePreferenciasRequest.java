package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Preferencias regionales del usuario (idioma, zona horaria, moneda).
 * Valores concretos se validan en servicio (listas blancas + ZoneId).
 */
public record UpdatePreferenciasRequest(
        @NotBlank(message = "El idioma es obligatorio")
        @Size(max = 10)
        String locale,

        @NotBlank(message = "La zona horaria es obligatoria")
        @Size(max = 64)
        String timeZone,

        @NotBlank(message = "La moneda es obligatoria")
        @Size(min = 3, max = 3)
        String currencyCode
) {}
