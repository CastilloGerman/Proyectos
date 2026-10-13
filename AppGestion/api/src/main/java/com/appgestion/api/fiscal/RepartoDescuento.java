package com.appgestion.api.fiscal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Reparto del descuento anterior al IVA sobre las bases de cada línea.
 * <p>
 * El porcentaje sale de la base neta redondeada a céntimo,
 * {@code suma × (1 − porcentaje / 100)} con HALF_UP, no del descuento redondeado
 * por separado. Ese descuento se reparte entre todas las líneas. El fijo va después,
 * sobre las bases ya escaladas: primero las de tipo distinto de cero y el sobrante
 * sobre las del 0 %. Cada parte del reparto se trunca a céntimo y los céntimos que
 * faltan se reparten de uno en uno a las líneas con mayor resto fraccionario; si empatan,
 * a la de mayor base y, si siguen empatando, a la primera.
 * Un porcentaje mayor que 100 deja todas las bases a cero. Una base, un porcentaje
 * o un fijo negativos lanzan {@link IllegalArgumentException}.
 */
public final class RepartoDescuento {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    public record Linea(BigDecimal base, int tipoIva) {
    }

    private RepartoDescuento() {
    }

    public static List<BigDecimal> repartir(List<Linea> lineas, BigDecimal porcentaje, BigDecimal fijo) {
        if (lineas == null || porcentaje == null || fijo == null) {
            throw new IllegalArgumentException("lineas, porcentaje y fijo son obligatorios");
        }
        if (porcentaje.signum() < 0) {
            throw new IllegalArgumentException("el porcentaje no puede ser negativo");
        }
        if (fijo.signum() < 0) {
            throw new IllegalArgumentException("el fijo no puede ser negativo");
        }
        int n = lineas.size();
        BigDecimal[] bases = new BigDecimal[n];
        int[] tipos = new int[n];
        for (int i = 0; i < n; i++) {
            Linea linea = lineas.get(i);
            if (linea == null || linea.base() == null) {
                throw new IllegalArgumentException("cada línea y su base son obligatorias");
            }
            if (linea.base().signum() < 0) {
                throw new IllegalArgumentException("la base no puede ser negativa");
            }
            bases[i] = linea.base().setScale(SCALE, ROUNDING);
            tipos[i] = linea.tipoIva();
        }
        aplicarPorcentaje(bases, porcentaje);
        aplicarFijo(bases, tipos, fijo);
        List<BigDecimal> out = new ArrayList<>(n);
        for (BigDecimal base : bases) {
            out.add(base);
        }
        return List.copyOf(out);
    }

    private static void aplicarPorcentaje(BigDecimal[] bases, BigDecimal porcentaje) {
        if (porcentaje.signum() == 0) {
            return;
        }
        BigDecimal suma = CERO;
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < bases.length; i++) {
            suma = suma.add(bases[i]);
            if (bases[i].signum() > 0) {
                indices.add(i);
            }
        }
        if (suma.signum() == 0) {
            return;
        }
        BigDecimal neto = porcentaje.compareTo(CIEN) >= 0
                ? CERO
                : suma.multiply(CIEN.subtract(porcentaje)).divide(CIEN, SCALE, ROUNDING);
        if (neto.signum() < 0) {
            neto = CERO;
        }
        if (neto.compareTo(suma) > 0) {
            neto = suma;
        }
        repartirSobre(bases, indices, suma.subtract(neto));
    }

    private static void aplicarFijo(BigDecimal[] bases, int[] tipos, BigDecimal fijo) {
        if (fijo.signum() <= 0) {
            return;
        }
        BigDecimal pendiente = fijo.setScale(SCALE, ROUNDING);
        pendiente = repartirSobre(bases, indicesConBase(bases, tipos, false), pendiente);
        repartirSobre(bases, indicesConBase(bases, tipos, true), pendiente);
    }

    private static List<Integer> indicesConBase(BigDecimal[] bases, int[] tipos, boolean alCero) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < bases.length; i++) {
            boolean esCero = tipos[i] == TipoIva.CERO;
            if (esCero == alCero && bases[i].signum() > 0) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * @return la parte de {@code cantidad} que no se ha podido descontar
     */
    private static BigDecimal repartirSobre(BigDecimal[] bases, List<Integer> indices, BigDecimal cantidad) {
        if (cantidad.signum() <= 0 || indices.isEmpty()) {
            return cantidad.signum() < 0 ? CERO : cantidad;
        }
        BigDecimal pool = CERO;
        for (int i : indices) {
            pool = pool.add(bases[i]);
        }
        if (pool.signum() == 0) {
            return cantidad;
        }
        BigDecimal objetivo = cantidad.min(pool).setScale(SCALE, ROUNDING);
        int m = indices.size();
        long objetivoCent = objetivo.movePointRight(SCALE).longValueExact();
        long poolCent = pool.movePointRight(SCALE).longValueExact();
        long[] parteCent = new long[m];
        long[] fraccion = new long[m];
        long sumaCent = 0;
        for (int k = 0; k < m; k++) {
            long baseCent = bases[indices.get(k)].movePointRight(SCALE).longValueExact();
            long numerador = Math.multiplyExact(objetivoCent, baseCent);
            parteCent[k] = numerador / poolCent;
            fraccion[k] = numerador % poolCent;
            sumaCent += parteCent[k];
        }
        long faltan = objetivoCent - sumaCent;
        List<Integer> orden = new ArrayList<>();
        for (int k = 0; k < m; k++) {
            orden.add(k);
        }
        orden.sort((a, b) -> {
            int cmp = Long.compare(fraccion[b], fraccion[a]);
            if (cmp != 0) {
                return cmp;
            }
            cmp = bases[indices.get(b)].compareTo(bases[indices.get(a)]);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(indices.get(a), indices.get(b));
        });
        for (long c = 0; c < faltan; c++) {
            parteCent[orden.get((int) (c % m))]++;
        }
        BigDecimal[] partes = new BigDecimal[m];
        for (int k = 0; k < m; k++) {
            partes[k] = BigDecimal.valueOf(parteCent[k], SCALE);
        }
        BigDecimal aplicado = CERO;
        for (int k = 0; k < indices.size(); k++) {
            int i = indices.get(k);
            BigDecimal antes = bases[i];
            BigDecimal despues = antes.subtract(partes[k]);
            if (despues.signum() < 0) {
                despues = CERO;
            }
            bases[i] = despues;
            aplicado = aplicado.add(antes.subtract(despues));
        }
        BigDecimal resto = cantidad.subtract(aplicado);
        return resto.signum() < 0 ? CERO : resto;
    }
}
