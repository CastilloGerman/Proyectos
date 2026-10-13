package com.appgestion.api.fiscal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Totales de una factura agrupando las líneas por tipo de IVA.
 * <p>
 * Cada grupo redondea su cuota a céntimo ({@code base del grupo × tipo}, HALF_UP).
 * La cuota de cabecera es la suma de esas cuotas. Dentro del grupo, cada línea
 * recibe una parte proporcional, redondeada otra vez: esa segunda pasada es la que
 * puede separar la suma de líneas de {@code facturas.iva} (por ejemplo 21,00 frente a 21,02).
 * <p>
 * Con un solo tipo distinto de cero el resultado coincide con el cálculo anterior,
 * que hacía un único redondeo de la base gravada × 0,21 y luego repartía. Una factura
 * ya emitida no se recalcula al migrar: el céntimo solo puede cambiar al guardar
 * de nuevo una factura con más de un tipo positivo (21 y 10, 21 y 4, o 10 y 4).
 */
public final class CalculoFactura {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public record LineaEntrada(BigDecimal subtotal, int tipoIva) {
    }

    public record Resultado(
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            List<Integer> tipos,
            List<BigDecimal> cuotas
    ) {
    }

    private CalculoFactura() {
    }

    /**
     * @param anticipoDescontado importe a restar del total a pagar. No modifica base ni IVA.
     *                           Null si la factura no descuenta un anticipo.
     */
    public static Resultado calcular(List<LineaEntrada> lineas, boolean ivaHabilitado, BigDecimal anticipoDescontado) {
        List<LineaEntrada> safe = lineas == null ? List.of() : lineas;
        int n = safe.size();
        int[] tipos = new int[n];
        BigDecimal[] bases = new BigDecimal[n];
        BigDecimal[] cuotas = new BigDecimal[n];
        BigDecimal subtotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

        for (int i = 0; i < n; i++) {
            LineaEntrada linea = safe.get(i);
            BigDecimal base = money(linea == null ? null : linea.subtotal());
            bases[i] = base;
            subtotal = subtotal.add(base);
            tipos[i] = ivaHabilitado
                    ? TipoIva.exigir(linea == null ? TipoIva.GENERAL : linea.tipoIva())
                    : TipoIva.CERO;
            cuotas[i] = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        subtotal = subtotal.setScale(SCALE, ROUNDING);

        Map<Integer, List<Integer>> grupos = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            grupos.computeIfAbsent(tipos[i], ignored -> new ArrayList<>()).add(i);
        }

        BigDecimal iva = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        for (Map.Entry<Integer, List<Integer>> grupo : grupos.entrySet()) {
            int tipo = grupo.getKey();
            if (tipo == TipoIva.CERO) {
                continue;
            }
            BigDecimal baseGrupo = BigDecimal.ZERO;
            for (int idx : grupo.getValue()) {
                baseGrupo = baseGrupo.add(bases[idx]);
            }
            baseGrupo = baseGrupo.setScale(SCALE, ROUNDING);
            BigDecimal cuotaGrupo = baseGrupo.multiply(TipoIva.tasa(tipo)).setScale(SCALE, ROUNDING);
            iva = iva.add(cuotaGrupo);
            if (baseGrupo.compareTo(BigDecimal.ZERO) > 0) {
                for (int idx : grupo.getValue()) {
                    BigDecimal proporcion = bases[idx].divide(baseGrupo, 10, ROUNDING);
                    cuotas[idx] = cuotaGrupo.multiply(proporcion).setScale(SCALE, ROUNDING);
                }
            }
        }
        iva = iva.setScale(SCALE, ROUNDING);

        BigDecimal bruto = subtotal.add(iva).setScale(SCALE, ROUNDING);
        BigDecimal total = anticipoDescontado == null
                ? bruto
                : bruto.subtract(money(anticipoDescontado)).setScale(SCALE, ROUNDING);

        List<Integer> tiposOut = new ArrayList<>(n);
        List<BigDecimal> cuotasOut = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            tiposOut.add(tipos[i]);
            cuotasOut.add(cuotas[i]);
        }
        return new Resultado(subtotal, iva, total, List.copyOf(tiposOut), List.copyOf(cuotasOut));
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        return value.setScale(SCALE, ROUNDING);
    }
}
