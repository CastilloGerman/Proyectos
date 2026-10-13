package com.appgestion.api.fiscal;

import java.math.BigDecimal;

/**
 * Qué cifra enseña el resumen del trimestre.
 * Cero facturas y cero gastos es «sin datos». Un resultado 0 € con movimientos
 * (factura a 0 €, o IVA repercutido igual al soportado) sigue siendo «a ingresar».
 */
public final class CifraModelo303 {

    public static final String SIN_DATOS = "Sin datos en este trimestre";
    public static final String A_INGRESAR = "IVA estimado a ingresar";
    public static final String A_COMPENSAR = "IVA estimado a compensar";

    private CifraModelo303() {
    }

    public static String etiqueta(long numeroFacturas, long numeroGastos, BigDecimal resultadoIva) {
        if (numeroFacturas == 0 && numeroGastos == 0) {
            return SIN_DATOS;
        }
        if (resultadoIva != null && resultadoIva.signum() < 0) {
            return A_COMPENSAR;
        }
        return A_INGRESAR;
    }

    /** False solo en el trimestre vacío. Un resultado de 0 € con datos sí se muestra. */
    public static boolean muestraImporte(long numeroFacturas, long numeroGastos) {
        return numeroFacturas != 0 || numeroGastos != 0;
    }
}
