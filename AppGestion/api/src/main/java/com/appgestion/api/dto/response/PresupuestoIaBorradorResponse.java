package com.appgestion.api.dto.response;

import java.util.List;

/** Borrador transitorio; generar esta respuesta no crea ni modifica entidades. */
public record PresupuestoIaBorradorResponse(
        Long clienteId,
        String clienteNombre,
        String clienteTelefono,
        String transcripcion,
        List<PresupuestoIaItemBorradorResponse> items,
        String notaAdicional
) {}
