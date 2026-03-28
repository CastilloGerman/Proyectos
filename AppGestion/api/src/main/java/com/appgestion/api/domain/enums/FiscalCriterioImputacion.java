package com.appgestion.api.domain.enums;

/**
 * Criterio para imputar facturas al trimestre del resumen fiscal orientativo.
 */
public enum FiscalCriterioImputacion {
    /** Fecha de expedición (o operación / creación como respaldo). */
    DEVENGO,
    /** Fecha del último cobro registrado; solo facturas marcadas como Pagada. */
    CAJA
}
