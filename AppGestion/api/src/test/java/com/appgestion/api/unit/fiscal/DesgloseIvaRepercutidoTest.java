package com.appgestion.api.unit.fiscal;

import com.appgestion.api.fiscal.CalculoFactura;
import com.appgestion.api.fiscal.CalculoFactura.LineaEntrada;
import com.appgestion.api.fiscal.DesgloseIvaRepercutido;
import com.appgestion.api.fiscal.DesgloseIvaRepercutido.Desglose;
import com.appgestion.api.fiscal.DesgloseIvaRepercutido.Linea;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DesgloseIvaRepercutidoTest {

    @Test
    void diezLineas_ajustePositivoYCuadraConLaCabecera() {
        CalculoFactura.Resultado factura = diezLineas();
        Desglose desglose = DesgloseIvaRepercutido.calcular(lineasDe(factura), factura.iva());

        assertThat(desglose.ajusteRedondeo()).isEqualByComparingTo("0.02");
        assertCuadra(desglose, factura.iva());
    }

    @Test
    void tresLineasDe003_ajusteNegativoYCuadraConLaCabecera() {
        List<LineaEntrada> entradas = List.of(
                new LineaEntrada(new BigDecimal("0.03"), 21),
                new LineaEntrada(new BigDecimal("0.03"), 21),
                new LineaEntrada(new BigDecimal("0.03"), 21)
        );
        CalculoFactura.Resultado factura = CalculoFactura.calcular(entradas, true, null);
        Desglose desglose = DesgloseIvaRepercutido.calcular(lineasDe(factura), factura.iva());

        assertThat(desglose.ajusteRedondeo().signum()).isNegative();
        assertThat(desglose.ajusteRedondeo()).isEqualByComparingTo("-0.01");
        assertCuadra(desglose, factura.iva());
    }

    @Test
    void anticipoNoEntraEnElDesglose() {
        CalculoFactura.Resultado factura = CalculoFactura.calcular(
                List.of(new LineaEntrada(new BigDecimal("100.00"), 21)),
                true,
                new BigDecimal("30.00")
        );
        Desglose desglose = DesgloseIvaRepercutido.calcular(lineasDe(factura), factura.iva());

        assertThat(factura.total()).isEqualByComparingTo("91.00");
        assertThat(desglose.ajusteRedondeo()).isEqualByComparingTo("0.00");
        assertThat(desglose.filas()).singleElement().satisfies(fila ->
                assertThat(fila.cuota()).isEqualByComparingTo("21.00"));
        assertCuadra(desglose, factura.iva());
    }

    private static CalculoFactura.Resultado diezLineas() {
        List<LineaEntrada> lineas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lineas.add(new LineaEntrada(new BigDecimal("10.01"), 21));
        }
        return CalculoFactura.calcular(lineas, true, null);
    }

    private static List<Linea> lineasDe(CalculoFactura.Resultado factura) {
        List<Linea> lineas = new ArrayList<>();
        for (int i = 0; i < factura.tipos().size(); i++) {
            lineas.add(new Linea(factura.tipos().get(i), BigDecimal.ZERO, factura.cuotas().get(i)));
        }
        return lineas;
    }

    private static void assertCuadra(Desglose desglose, BigDecimal ivaCabecera) {
        BigDecimal suma = desglose.filas().stream().map(DesgloseIvaRepercutido.Fila::cuota).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(suma.add(desglose.ajusteRedondeo())).isEqualByComparingTo(ivaCabecera);
    }
}
