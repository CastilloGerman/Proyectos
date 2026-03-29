package com.appgestion.api.dto.response;

/**
 * Una condición del catálogo para mostrar en UI (sin lógica en el cliente).
 */
public record PresupuestoCondicionDisponibleResponse(
        String clave,
        String textoVisible,
        boolean activaPorDefecto
) {}
