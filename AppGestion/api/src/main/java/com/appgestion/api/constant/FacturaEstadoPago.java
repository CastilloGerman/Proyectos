package com.appgestion.api.constant;

/**
 * Valores persistidos en {@code facturas.estado_pago} (texto libre en BD; la app usa estos literales).
 */
public final class FacturaEstadoPago {

    private FacturaEstadoPago() {
    }

    public static final String PAGADA = "Pagada";
    public static final String NO_PAGADA = "No Pagada";
    public static final String PARCIAL = "Parcial";
}
