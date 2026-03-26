package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Activa o configura recordatorios automáticos por email al cliente (facturas impagadas).
 * Los días son <strong>después de la fecha de vencimiento</strong> (ej. 7 = una semana de retraso).
 */
public record RecordatorioCobroPatchRequest(
        @NotNull Boolean recordatorioClienteActivo,
        @NotNull List<Integer> recordatorioClienteDias
) {}
