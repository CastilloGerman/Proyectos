package com.appgestion.api.fiscal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Desglose del IVA repercutido por tipo de línea.
 * <p>
 * El 303 suma {@code facturas.iva}. Las filas suman {@code factura_items.cuota_iva}.
 * La diferencia es el ajuste por redondeo (positivo si la cabecera queda por encima
 * de las líneas, negativo si las cuotas de línea redondeadas suman de más).
 * Filas + ajuste = IVA repercutido, al céntimo.
 * <p>
 * La fila de tipo 0, cuando aparece, junta exento y gravado al 0 %. Ver {@link TipoIva}.
 */
public final class DesgloseIvaRepercutido {

    private static final List<Integer> ORDEN = List.of(
            TipoIva.GENERAL, TipoIva.REDUCIDO, TipoIva.SUPERREDUCIDO, TipoIva.CERO);

    public record Linea(int tipoIva, BigDecimal base, BigDecimal cuota) {
    }

    public record Fila(int tipoIva, String etiqueta, BigDecimal base, BigDecimal cuota) {
    }

    public record Desglose(List<Fila> filas, BigDecimal ajusteRedondeo) {
    }

    private DesgloseIvaRepercutido() {
    }

    public static Desglose calcular(List<Linea> lineas, BigDecimal ivaCabeceras) {
        Map<Integer, BigDecimal> bases = new LinkedHashMap<>();
        Map<Integer, BigDecimal> cuotas = new LinkedHashMap<>();
        for (int tipo : ORDEN) {
            bases.put(tipo, cero());
            cuotas.put(tipo, cero());
        }
        if (lineas != null) {
            for (Linea linea : lineas) {
                if (linea == null) {
                    continue;
                }
                int tipo = TipoIva.exigir(linea.tipoIva());
                bases.put(tipo, bases.get(tipo).add(money(linea.base())));
                cuotas.put(tipo, cuotas.get(tipo).add(money(linea.cuota())));
            }
        }

        List<Fila> filas = new ArrayList<>();
        BigDecimal sumaCuotas = cero();
        for (int tipo : ORDEN) {
            BigDecimal base = bases.get(tipo).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cuota = cuotas.get(tipo).setScale(2, RoundingMode.HALF_UP);
            sumaCuotas = sumaCuotas.add(cuota);
            if (base.signum() != 0 || cuota.signum() != 0) {
                filas.add(new Fila(tipo, TipoIva.etiquetaDesglose(tipo), base, cuota));
            }
        }
        BigDecimal ajuste = money(ivaCabeceras).subtract(sumaCuotas).setScale(2, RoundingMode.HALF_UP);
        return new Desglose(List.copyOf(filas), ajuste);
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return cero();
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
