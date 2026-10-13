package com.appgestion.api.unit.fiscal;

import com.appgestion.api.fiscal.RepartoDescuento;
import com.appgestion.api.fiscal.RepartoDescuento.Linea;
import com.appgestion.api.fiscal.TipoIva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El reparto es anterior al IVA: primero el porcentaje sobre todas las líneas,
 * después el fijo sobre las de tipo distinto de cero y el sobrante sobre las del 0 %.
 * Cada parte se trunca a céntimo. Los céntimos que faltan van, de uno en uno,
 * a la línea con mayor resto fraccionario. Si empatan, a la de mayor base y,
 * si siguen empatando, a la primera.
 */
class RepartoDescuentoTest {

    @Test
    void tipos21_10_4_elCentimoDeMasCaeEnLaLineaDeMayorBase() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("33.33", TipoIva.GENERAL),
                linea("33.33", TipoIva.REDUCIDO),
                linea("33.34", TipoIva.SUPERREDUCIDO),
                linea("10.00", TipoIva.CERO)
        ), new BigDecimal("0"), new BigDecimal("1.00"));

        assertEuros(bases, "33.00", "33.00", "33.00", "10.00");
        assertThat(suma(bases)).isEqualByComparingTo("109.00");
    }

    @Test
    void redondeo_laSumaCuadraYElRestoCaeEnLaMayorBase() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("10.00", TipoIva.GENERAL),
                linea("10.00", TipoIva.GENERAL),
                linea("10.01", TipoIva.GENERAL)
        ), new BigDecimal("0"), new BigDecimal("1.00"));

        assertEuros(bases, "9.67", "9.67", "9.67");
        assertThat(suma(bases)).isEqualByComparingTo("29.01");
    }

    /**
     * Cada parte se trunca a céntimo. 0,005 € queda en 0,00 € y el resto fraccionario
     * es el mismo en las dos líneas. El céntimo que falta se reparte de uno en uno:
     * empatan en resto y en base, así que se lo lleva la primera. Resultado 9,99 y 10,00.
     */
    @Test
    void empate_halfUpDelDescuentoQuitaElCentimoDeMasDeLaPrimeraLinea() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("10.00", TipoIva.GENERAL),
                linea("10.00", TipoIva.REDUCIDO)
        ), new BigDecimal("0"), new BigDecimal("0.01"));

        assertEuros(bases, "9.99", "10.00");
    }

    @Test
    void fijoConTodasLasLineasAlCero_noDividePorLaBaseGravada() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("40.00", TipoIva.CERO),
                linea("60.00", TipoIva.CERO)
        ), new BigDecimal("0"), new BigDecimal("10.00"));

        assertEuros(bases, "36.00", "54.00");
        assertThat(suma(bases)).isEqualByComparingTo("90.00");
    }

    @Test
    void baseGravadaCero_elFijoPasaEnteroALasLineasAlCero() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("0.00", TipoIva.GENERAL),
                linea("100.00", TipoIva.CERO)
        ), new BigDecimal("0"), new BigDecimal("10.00"));

        assertEuros(bases, "0.00", "90.00");
        assertThat(suma(bases)).isEqualByComparingTo("90.00");
    }

    @Test
    void fijoMayorQueTodaLaBase_seLimitaACero() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("40.00", TipoIva.GENERAL),
                linea("60.00", TipoIva.CERO)
        ), new BigDecimal("0"), new BigDecimal("200.00"));

        assertEuros(bases, "0.00", "0.00");
        assertThat(bases).allSatisfy(base -> assertThat(base).isGreaterThanOrEqualTo(BigDecimal.ZERO));
    }

    @Test
    void fijoMayorQueLaLineaPequena_noDejaBaseNegativa() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("5.00", TipoIva.GENERAL),
                linea("100.00", TipoIva.GENERAL)
        ), new BigDecimal("0"), new BigDecimal("30.00"));

        assertEuros(bases, "3.57", "71.43");
        assertThat(bases).allSatisfy(base -> assertThat(base).isGreaterThanOrEqualTo(BigDecimal.ZERO));
        assertThat(suma(bases)).isEqualByComparingTo("75.00");
    }

    @Test
    void fijoMayorQueLaBaseGravada_elSobrantePasaAlCero() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("40.00", TipoIva.GENERAL),
                linea("50.00", TipoIva.REDUCIDO),
                linea("100.00", TipoIva.CERO)
        ), new BigDecimal("0"), new BigDecimal("100.00"));

        assertEuros(bases, "0.00", "0.00", "90.00");
        assertThat(bases).allSatisfy(base -> assertThat(base).isGreaterThanOrEqualTo(BigDecimal.ZERO));
        assertThat(suma(bases)).isEqualByComparingTo("90.00");
    }

    @Test
    void porcentajePrimeroYDespuesElFijoSobreLoYaEscalado() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("100.00", TipoIva.GENERAL),
                linea("100.00", TipoIva.CERO)
        ), new BigDecimal("10"), new BigDecimal("10.00"));

        assertEuros(bases, "80.00", "90.00");
        assertThat(suma(bases)).isEqualByComparingTo("170.00");
    }

    /**
     * 3 × 10,05 = 30,15. La base neta del presupuesto es 30,15 × 0,9 = 27,135,
     * que a céntimo (half up) es 27,14. El descuento a repartir es 3,01.
     * Cada parte half up sale 1,00 y el céntimo de más se quita de la primera línea.
     */
    @Test
    void tresLineasDe1005AlDiezPorCiento_laSumaCuadraAlCentimoConLaBaseNeta() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("10.05", TipoIva.GENERAL),
                linea("10.05", TipoIva.GENERAL),
                linea("10.05", TipoIva.GENERAL)
        ), new BigDecimal("10"), BigDecimal.ZERO);

        assertEuros(bases, "9.04", "9.05", "9.05");
        assertThat(suma(bases)).isEqualByComparingTo("27.14");
    }

    @Test
    void porcentajeSuperiorA100_dejaTodasLasBasesACero() {
        List<BigDecimal> bases = RepartoDescuento.repartir(List.of(
                linea("10.05", TipoIva.GENERAL),
                linea("40.00", TipoIva.CERO)
        ), new BigDecimal("150"), BigDecimal.ZERO);

        assertEuros(bases, "0.00", "0.00");
        assertThat(suma(bases)).isEqualByComparingTo("0.00");
    }

    @Test
    void basePorcentajeOFijoNegativos_lanzanIllegalArgumentException() {
        assertThatThrownBy(() -> RepartoDescuento.repartir(
                List.of(linea("-1.00", TipoIva.GENERAL)), BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RepartoDescuento.repartir(
                List.of(linea("10.00", TipoIva.GENERAL)), new BigDecimal("-10"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RepartoDescuento.repartir(
                List.of(linea("10.00", TipoIva.GENERAL)), BigDecimal.ZERO, new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cienLineasDeDiezCentimosAlCincoPorCiento_laSumaEs950() {
        List<Linea> lineas = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            lineas.add(linea("0.10", TipoIva.GENERAL));
        }

        List<BigDecimal> bases = RepartoDescuento.repartir(lineas, new BigDecimal("5"), BigDecimal.ZERO);

        assertThat(suma(bases)).isEqualByComparingTo("9.50");
    }

    @Test
    void veinteLineasDeUnEuroAlDosYMedioPorCiento_laSumaEs1950() {
        List<Linea> lineas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            lineas.add(linea("1.00", TipoIva.GENERAL));
        }

        List<BigDecimal> bases = RepartoDescuento.repartir(lineas, new BigDecimal("2.5"), BigDecimal.ZERO);

        assertThat(suma(bases)).isEqualByComparingTo("19.50");
    }

    /**
     * Semillas fijas, impresas en el fallo para repetir el caso. Además de líneas
     * distintas hay tandas de líneas iguales (mismo importe, N entre 2 y 100).
     * La base que vería el cliente es el double del presupuesto impreso como el PDF,
     * {@code %.2f}. No hay tolerancia: un céntimo de diferencia cuenta.
     */
    @Test
    void propiedades_conSemillaFija_respetanLosInvariantes() {
        long[] semillas = {20261013L, 1L, 7L, 42L, 99L};
        int[] tipos = {TipoIva.GENERAL, TipoIva.REDUCIDO, TipoIva.SUPERREDUCIDO, TipoIva.CERO};
        List<String> rotos = new ArrayList<>();
        List<String> cientosDistintos = new ArrayList<>();

        for (long semilla : semillas) {
            Random rnd = new Random(semilla);
            for (int caso = 0; caso < 200; caso++) {
                int n = 1 + rnd.nextInt(30);
                List<Linea> lineas = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    lineas.add(new Linea(
                            BigDecimal.valueOf(1 + rnd.nextInt(50_000), 2),
                            tipos[rnd.nextInt(tipos.length)]));
                }
                comprobarCaso(semilla, "mixto", caso, lineas, rnd, rotos, cientosDistintos);
            }
            for (int caso = 0; caso < 50; caso++) {
                int n = 2 + rnd.nextInt(99);
                BigDecimal importe = BigDecimal.valueOf(1 + rnd.nextInt(50_000), 2);
                List<Linea> lineas = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    lineas.add(new Linea(importe, TipoIva.GENERAL));
                }
                comprobarCaso(semilla, "iguales", caso, lineas, rnd, rotos, cientosDistintos);
            }
        }

        String semillasTexto = "semillas 20261013, 1, 7, 42, 99";
        assertThat(rotos).as(semillasTexto + "\n" + String.join("\n", rotos.subList(0, Math.min(rotos.size(), 12)))).isEmpty();
        assertThat(cientosDistintos)
                .as(semillasTexto + "\n" + cientosDistintos.size() + " casos\n" + String.join("\n",
                        cientosDistintos.subList(0, Math.min(cientosDistintos.size(), 12))))
                .isEmpty();
    }

    private static void comprobarCaso(
            long semilla,
            String tanda,
            int caso,
            List<Linea> lineas,
            Random rnd,
            List<String> rotos,
            List<String> cientosDistintos) {
        BigDecimal sumaEntrada = suma(lineas.stream().map(Linea::base).toList());
        int modo = rnd.nextInt(3);
        BigDecimal porcentaje = modo == 1 ? BigDecimal.ZERO : BigDecimal.valueOf(rnd.nextInt(15_001), 2);
        BigDecimal fijo = modo == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(rnd.nextLong(sumaEntrada.movePointRight(2).longValueExact() * 3 / 2 + 1), 2);
        String donde = "semilla " + semilla + " tanda " + tanda + " caso " + caso;
        try {
            List<BigDecimal> soloPorcentaje = RepartoDescuento.repartir(lineas, porcentaje, BigDecimal.ZERO);
            List<BigDecimal> salida = RepartoDescuento.repartir(lineas, porcentaje, fijo);
            BigDecimal sumaSalida = suma(salida);
            for (int i = 0; i < lineas.size(); i++) {
                if (salida.get(i).signum() < 0 || salida.get(i).compareTo(lineas.get(i).base()) > 0) {
                    rotos.add(donde + " línea " + i + " fuera de [0, base]: " + salida.get(i));
                }
            }
            BigDecimal esperada = baseNetaEsperada(sumaEntrada, porcentaje)
                    .subtract(fijo).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            if (sumaSalida.compareTo(esperada) != 0) {
                rotos.add(donde + " suma " + sumaSalida + " esperada " + esperada
                        + " (p=" + porcentaje + ", fijo=" + fijo + ", entrada=" + sumaEntrada + ", n=" + lineas.size() + ")");
            }
            boolean gravadaConBase = false;
            for (int i = 0; i < lineas.size(); i++) {
                if (lineas.get(i).tipoIva() != TipoIva.CERO && salida.get(i).signum() > 0) {
                    gravadaConBase = true;
                    break;
                }
            }
            if (gravadaConBase) {
                for (int i = 0; i < lineas.size(); i++) {
                    if (lineas.get(i).tipoIva() == TipoIva.CERO
                            && salida.get(i).compareTo(soloPorcentaje.get(i)) != 0) {
                        rotos.add(donde + " el fijo toca la línea " + i + " al 0 % con gravadas aún con base");
                    }
                }
            }
            double visto = baseComoLaVeElCliente(lineas, porcentaje, fijo);
            BigDecimal cliente = new BigDecimal(String.format(Locale.US, "%.2f", visto));
            if (cliente.compareTo(sumaSalida) != 0) {
                cientosDistintos.add(donde + " factura " + sumaSalida + " cliente " + cliente
                        + " double " + visto + " (p=" + porcentaje + ", fijo=" + fijo + ", n=" + lineas.size() + ")");
            }
        } catch (RuntimeException ex) {
            rotos.add(donde + " " + ex);
        }
    }

    private static BigDecimal baseNetaEsperada(BigDecimal suma, BigDecimal porcentaje) {
        if (porcentaje.compareTo(new BigDecimal("100")) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return suma.multiply(new BigDecimal("100").subtract(porcentaje))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /** Misma cuenta en double que el presupuesto, la que luego imprimen el PDF y la pantalla. */
    private static double baseComoLaVeElCliente(List<Linea> lineas, BigDecimal porcentaje, BigDecimal fijo) {
        double subtotal = 0;
        for (Linea linea : lineas) {
            subtotal += linea.base().doubleValue();
        }
        subtotal = subtotal * (1 - porcentaje.doubleValue() / 100) - fijo.doubleValue();
        return Math.max(0, subtotal);
    }

    private static Linea linea(String base, int tipo) {
        return new Linea(new BigDecimal(base), tipo);
    }

    private static void assertEuros(List<BigDecimal> actual, String... expected) {
        assertThat(actual).hasSize(expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertThat(actual.get(i)).isEqualByComparingTo(expected[i]);
        }
    }

    private static BigDecimal suma(List<BigDecimal> bases) {
        return bases.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
