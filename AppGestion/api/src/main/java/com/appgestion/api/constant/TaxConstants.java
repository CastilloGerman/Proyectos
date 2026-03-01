package com.appgestion.api.constant;

import java.math.BigDecimal;

/**
 * Constantes fiscales compartidas (IVA español).
 */
public final class TaxConstants {

    private TaxConstants() {}

    /** IVA general en España: 21% */
    public static final double IVA_RATE_DOUBLE = 0.21;

    /** IVA general en España: 21% (para cálculos con BigDecimal) */
    public static final BigDecimal IVA_RATE = new BigDecimal("0.21");

    /** Porcentaje IVA para mostrar en UI/PDF */
    public static final String IVA_PERCENT_LABEL = "21%";
}
