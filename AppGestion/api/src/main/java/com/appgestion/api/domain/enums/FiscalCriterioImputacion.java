package com.appgestion.api.domain.enums;

/**
 * Criterio para imputar facturas al trimestre del resumen fiscal orientativo.
 */
public enum FiscalCriterioImputacion {
    /** Fecha de expedición (o operación / creación como respaldo). */
    DEVENGO,
    /**
     * Valor de API. En pantalla y PDF se muestra como «Por fecha de cobro».
     * No es el régimen especial de criterio de caja (RECC): usa la fecha del último
     * cobro y no prorratea cobros parciales.
     */
    CAJA
}
