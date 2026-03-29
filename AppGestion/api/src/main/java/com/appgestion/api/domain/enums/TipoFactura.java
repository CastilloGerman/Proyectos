package com.appgestion.api.domain.enums;

/**
 * Tipo de factura para el flujo de anticipos fiscales (dos facturas legales)
 * y facturación ordinaria.
 */
public enum TipoFactura {
    /** Facturación clásica (una factura por operación). */
    NORMAL,
    /** Factura del cobro del anticipo (base + IVA del tramo anticipado). */
    ANTICIPO,
    /** Factura final con base/IVA remanentes y descuento del anticipo ya facturado. */
    FINAL_CON_ANTICIPO
}
