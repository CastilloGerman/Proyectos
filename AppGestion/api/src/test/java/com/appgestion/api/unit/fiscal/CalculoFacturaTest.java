package com.appgestion.api.unit.fiscal;

import com.appgestion.api.fiscal.CalculoFactura;
import com.appgestion.api.fiscal.CalculoFactura.LineaEntrada;
import com.appgestion.api.fiscal.CalculoFactura.Resultado;
import com.appgestion.api.fiscal.TipoIva;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoFacturaTest {

    @Test
    void facturaAl21_coincideConLaFormulaAnterior() {
        Resultado nuevo = CalculoFactura.calcular(List.of(linea("100.00", 21)), true, null);
        Resultado anterior = formulaAnterior(List.of(linea("100.00", 21)), true, null);

        assertIguales(nuevo, anterior);
        assertThat(nuevo.iva()).isEqualByComparingTo("21.00");
        assertThat(nuevo.total()).isEqualByComparingTo("121.00");
    }

    @Test
    void facturaExenta_coincideConLaFormulaAnterior() {
        Resultado nuevo = CalculoFactura.calcular(List.of(linea("80.00", 0)), true, null);
        Resultado anterior = formulaAnterior(List.of(linea("80.00", 0)), true, null);

        assertIguales(nuevo, anterior);
        assertThat(nuevo.iva()).isEqualByComparingTo("0.00");
        assertThat(nuevo.total()).isEqualByComparingTo("80.00");
        assertThat(nuevo.tipos()).containsExactly(TipoIva.CERO);
    }

    @Test
    void ivaDesactivado_dejaCuotaCeroAunqueLaLineaPidiera21() {
        Resultado nuevo = CalculoFactura.calcular(List.of(linea("100.00", 21)), false, null);
        Resultado anterior = formulaAnterior(List.of(linea("100.00", 21)), false, null);

        assertIguales(nuevo, anterior);
        assertThat(nuevo.tipos()).containsExactly(TipoIva.CERO);
    }

    @Test
    void mixta21YExenta_coincideConLaFormulaAnterior() {
        List<LineaEntrada> lineas = List.of(linea("50.00", 21), linea("30.00", 0));

        assertIguales(
                CalculoFactura.calcular(lineas, true, null),
                formulaAnterior(lineas, true, null)
        );
    }

    @Test
    void diezLineasDe1001_laCabeceraSigueEn2102_yLasLineasEn2100() {
        List<LineaEntrada> lineas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lineas.add(linea("10.01", 21));
        }
        Resultado nuevo = CalculoFactura.calcular(lineas, true, null);
        Resultado anterior = formulaAnterior(lineas, true, null);

        assertIguales(nuevo, anterior);
        assertThat(nuevo.subtotal()).isEqualByComparingTo("100.10");
        assertThat(nuevo.iva()).isEqualByComparingTo("21.02");
        BigDecimal sumaLineas = nuevo.cuotas().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sumaLineas).isEqualByComparingTo("21.00");
        assertThat(nuevo.iva().subtract(sumaLineas)).isEqualByComparingTo("0.02");
    }

    @Test
    void anticipo_restaSoloDelTotal() {
        List<LineaEntrada> lineas = List.of(linea("100.00", 21));
        BigDecimal anticipo = new BigDecimal("30.00");
        Resultado nuevo = CalculoFactura.calcular(lineas, true, anticipo);
        Resultado anterior = formulaAnterior(lineas, true, anticipo);

        assertIguales(nuevo, anterior);
        assertThat(nuevo.iva()).isEqualByComparingTo("21.00");
        assertThat(nuevo.total()).isEqualByComparingTo("91.00");
    }

    @Test
    void tipo10_redondeaElGrupoAparte() {
        Resultado resultado = CalculoFactura.calcular(List.of(linea("100.00", 10)), true, null);

        assertThat(resultado.iva()).isEqualByComparingTo("10.00");
        assertThat(resultado.total()).isEqualByComparingTo("110.00");
        assertThat(resultado.cuotas()).containsExactly(new BigDecimal("10.00"));
    }

    @Test
    void tresLineasDe003_lasCuotasDeLineaSuperanLaCabecera() {
        List<LineaEntrada> lineas = List.of(linea("0.03", 21), linea("0.03", 21), linea("0.03", 21));
        Resultado resultado = CalculoFactura.calcular(lineas, true, null);
        BigDecimal sumaLineas = resultado.cuotas().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(resultado.iva()).isEqualByComparingTo("0.02");
        assertThat(sumaLineas).isEqualByComparingTo("0.03");
    }

    /**
     * Copia del cálculo anterior a agrupar por tipo: una sola cuota sobre la base
     * gravada al 21 % y reparto proporcional. Solo sirve para la regresión de un tipo.
     */
    private static Resultado formulaAnterior(List<LineaEntrada> lineas, boolean ivaHabilitado, BigDecimal anticipo) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal baseIva = BigDecimal.ZERO;
        for (LineaEntrada linea : lineas) {
            BigDecimal base = linea.subtotal().setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(base);
            if (ivaHabilitado && linea.tipoIva() != TipoIva.CERO) {
                baseIva = baseIva.add(base);
            }
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal cuota = ivaHabilitado
                ? baseIva.multiply(new BigDecimal("0.21")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(cuota).setScale(2, RoundingMode.HALF_UP);
        if (anticipo != null) {
            total = total.subtract(anticipo).setScale(2, RoundingMode.HALF_UP);
        }
        List<Integer> tipos = new ArrayList<>();
        List<BigDecimal> cuotas = new ArrayList<>();
        for (LineaEntrada linea : lineas) {
            int tipo = ivaHabilitado && linea.tipoIva() != TipoIva.CERO ? TipoIva.GENERAL : TipoIva.CERO;
            tipos.add(tipo);
            BigDecimal cuotaLinea = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (ivaHabilitado && tipo != TipoIva.CERO && baseIva.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal proporcion = linea.subtotal().setScale(2, RoundingMode.HALF_UP)
                        .divide(baseIva, 10, RoundingMode.HALF_UP);
                cuotaLinea = cuota.multiply(proporcion).setScale(2, RoundingMode.HALF_UP);
            }
            cuotas.add(cuotaLinea);
        }
        return new Resultado(subtotal, cuota, total, tipos, cuotas);
    }

    private static void assertIguales(Resultado nuevo, Resultado anterior) {
        assertThat(nuevo.subtotal()).isEqualByComparingTo(anterior.subtotal());
        assertThat(nuevo.iva()).isEqualByComparingTo(anterior.iva());
        assertThat(nuevo.total()).isEqualByComparingTo(anterior.total());
        assertThat(nuevo.tipos()).isEqualTo(anterior.tipos());
        assertThat(nuevo.cuotas()).hasSameSizeAs(anterior.cuotas());
        for (int i = 0; i < nuevo.cuotas().size(); i++) {
            assertThat(nuevo.cuotas().get(i)).isEqualByComparingTo(anterior.cuotas().get(i));
        }
    }

    private static LineaEntrada linea(String subtotal, int tipo) {
        return new LineaEntrada(new BigDecimal(subtotal), tipo);
    }
}
