package com.appgestion.api.dto.response;

/**
 * Incidencia de calidad de un registro del periodo, con ruta de la app para abrirla.
 */
public record IncidenciaCalidadResponse(
        String tipo,
        long id,
        String etiqueta,
        String ruta
) {
}
