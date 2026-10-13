package com.appgestion.api.fiscal;

import java.math.BigDecimal;

/**
 * Tipos de IVA de una línea de factura: 21, 10, 4 o 0.
 * <p>
 * {@code tipo_iva = 0} mezcla dos supuestos que el Modelo 303 oficial separa en casillas distintas:
 * la operación exenta y la operación gravada al 0 %. La Fase 5 no puede volcar ambos a la misma
 * casilla sin distinguirlos antes.
 */
public final class TipoIva {

    public static final int GENERAL = 21;
    public static final int REDUCIDO = 10;
    public static final int SUPERREDUCIDO = 4;
    /** Exento y gravado al 0 % comparten este valor. Ver el javadoc de la clase. */
    public static final int CERO = 0;

    private TipoIva() {
    }

    /**
     * Si llega {@code tipoIva}, manda sobre {@code aplicaIva}. Si no llega, {@code aplicaIva}
     * false es 0 y cualquier otro caso es 21 (clientes que aún envían solo el checkbox).
     */
    public static int resolver(Integer tipoIva, Boolean aplicaIva) {
        if (tipoIva != null) {
            return exigir(tipoIva);
        }
        return Boolean.FALSE.equals(aplicaIva) ? CERO : GENERAL;
    }

    public static int exigir(int tipoIva) {
        if (tipoIva != GENERAL && tipoIva != REDUCIDO && tipoIva != SUPERREDUCIDO && tipoIva != CERO) {
            throw new IllegalArgumentException("tipo_iva debe ser 21, 10, 4 o 0");
        }
        return tipoIva;
    }

    public static BigDecimal tasa(int tipoIva) {
        return switch (exigir(tipoIva)) {
            case GENERAL -> new BigDecimal("0.21");
            case REDUCIDO -> new BigDecimal("0.10");
            case SUPERREDUCIDO -> new BigDecimal("0.04");
            default -> BigDecimal.ZERO;
        };
    }

    /** Etiqueta de línea en el PDF de factura: {@code 21%}, {@code 10%}, {@code 4%}, {@code 0%}. */
    public static String etiquetaLinea(int tipoIva) {
        return exigir(tipoIva) + "%";
    }

    /** Etiqueta del desglose del 303. */
    public static String etiquetaDesglose(int tipoIva) {
        return "IVA " + exigir(tipoIva) + " %";
    }
}
