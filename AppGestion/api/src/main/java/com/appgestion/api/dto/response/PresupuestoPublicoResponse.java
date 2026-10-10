package com.appgestion.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PresupuestoPublicoResponse(
        String empresaNombre,
        String empresaLogoBase64,
        String empresaLogoMimeType,
        Long numero,
        LocalDateTime fecha,
        String clienteNombre,
        List<PartidaPublica> partidas,
        Double subtotal,
        Double iva,
        Double total,
        String notas,
        List<String> condiciones,
        boolean permiteResponder,
        String respuestaCliente
) {
    public record PartidaPublica(String descripcion, Double cantidad, String unidad, Double precioUnitario, Double subtotal) {}
}
