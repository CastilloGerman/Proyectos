package com.appgestion.api.unit.fiscal;

import com.appgestion.api.fiscal.CifraModelo303;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CifraModelo303Test {

    @Test
    void sinFacturasNiGastos_noMuestraCeroComoIngreso() {
        assertThat(CifraModelo303.etiqueta(0, 0, BigDecimal.ZERO)).isEqualTo(CifraModelo303.SIN_DATOS);
        assertThat(CifraModelo303.muestraImporte(0, 0)).isFalse();
    }

    @Test
    void facturaDeCeroEuros_sigueSiendoIvaAIngresar() {
        assertThat(CifraModelo303.etiqueta(1, 0, new BigDecimal("0.00"))).isEqualTo(CifraModelo303.A_INGRESAR);
        assertThat(CifraModelo303.muestraImporte(1, 0)).isTrue();
    }

    @Test
    void repercutidoIgualASoportado_sigueSiendoIvaAIngresarEnCero() {
        BigDecimal resultado = new BigDecimal("21.00").subtract(new BigDecimal("21.00"));

        assertThat(resultado).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(CifraModelo303.etiqueta(4, 2, resultado)).isEqualTo(CifraModelo303.A_INGRESAR);
        assertThat(CifraModelo303.muestraImporte(4, 2)).isTrue();
    }

    @Test
    void resultadoNegativo_esACompensar() {
        assertThat(CifraModelo303.etiqueta(1, 1, new BigDecimal("-0.01"))).isEqualTo(CifraModelo303.A_COMPENSAR);
    }
}
