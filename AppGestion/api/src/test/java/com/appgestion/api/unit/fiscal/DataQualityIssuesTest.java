package com.appgestion.api.unit.fiscal;

import com.appgestion.api.fiscal.CalculoFactura;
import com.appgestion.api.fiscal.DataQualityIssues;
import com.appgestion.api.fiscal.DataQualityIssues.ExpenseSnapshot;
import com.appgestion.api.fiscal.DataQualityIssues.InvoiceSnapshot;
import com.appgestion.api.fiscal.DataQualityIssues.QualityIssue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DataQualityIssuesTest {

    @Test
    void lineasQueCuadran_noGeneranIncidencia() {
        InvoiceSnapshot factura = factura(1L, "F-1", 10L, "100.00", "21.00", "100.00", "21.00", 2);

        List<QualityIssue> issues = DataQualityIssues.getDataQualityIssues(List.of(factura), List.of(gastoOk()));

        assertThat(issues).isEmpty();
    }

    @Test
    void baseDistinta_marcaLineasQueNoCuadran() {
        InvoiceSnapshot factura = factura(7L, "F-7", 3L, "100.00", "21.00", "99.00", "21.00", 1);

        List<QualityIssue> issues = DataQualityIssues.getDataQualityIssues(List.of(factura), List.of());

        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.tipo()).isEqualTo(DataQualityIssues.LINEAS_NO_CUADRAN);
            assertThat(issue.id()).isEqualTo(7L);
            assertThat(issue.etiqueta()).isEqualTo("Factura F-7: las líneas no cuadran con el total");
            assertThat(issue.ruta()).isEqualTo("/facturas/7");
        });
    }

    @Test
    void ivaDentroDeUnCentimoPorLinea_noSeMarca() {
        InvoiceSnapshot factura = factura(2L, "F-2", 1L, "100.00", "21.00", "100.00", "21.03", 3);

        assertThat(DataQualityIssues.getDataQualityIssues(List.of(factura), List.of())).isEmpty();
    }

    @Test
    void ivaPorEncimaDeLaTolerancia_seMarca() {
        InvoiceSnapshot factura = factura(2L, "F-2", 1L, "100.00", "21.00", "100.00", "21.04", 3);

        assertThat(DataQualityIssues.getDataQualityIssues(List.of(factura), List.of()))
                .extracting(QualityIssue::tipo)
                .containsExactly(DataQualityIssues.LINEAS_NO_CUADRAN);
    }

    @Test
    void sinLineasYcabeceraACero_noSeMarca() {
        InvoiceSnapshot factura = factura(4L, "F-4", 1L, "0.00", "0.00", "0.00", "0.00", 0);

        assertThat(DataQualityIssues.getDataQualityIssues(List.of(factura), List.of())).isEmpty();
    }

    @Test
    void sinLineasYcabeceraConImporte_seMarca() {
        InvoiceSnapshot factura = factura(5L, "  ", 1L, "50.00", "0.00", "0.00", "0.00", 0);

        List<QualityIssue> issues = DataQualityIssues.getDataQualityIssues(List.of(factura), List.of());

        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.tipo()).isEqualTo(DataQualityIssues.LINEAS_NO_CUADRAN);
            assertThat(issue.etiqueta()).isEqualTo("Factura #5: las líneas no cuadran con el total");
        });
    }

    /**
     * Diez líneas de 10,01 €. El IVA de cabecera es 21,02 € y la suma de cuotas
     * redondeadas por línea es 21,00 €: 0,02 € de desvío. Cabe en 0,01 € × 10 líneas
     * y no cabe en un tope único de 0,01 €.
     */
    @Test
    void diezLineas_redondeoLegitimoPorLinea_noSeMarca() {
        List<CalculoFactura.LineaEntrada> lineas = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lineas.add(new CalculoFactura.LineaEntrada(new BigDecimal("10.01"), 21));
        }
        CalculoFactura.Resultado totales = CalculoFactura.calcular(lineas, true, null);
        BigDecimal sumaCuotas = totales.cuotas().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totales.iva()).isEqualByComparingTo("21.02");
        assertThat(sumaCuotas).isEqualByComparingTo("21.00");
        assertThat(totales.iva().subtract(sumaCuotas).abs()).isGreaterThan(new BigDecimal("0.01"));

        InvoiceSnapshot factura = new InvoiceSnapshot(
                10L, "F-10", 1L, totales.subtotal(), totales.iva(), totales.subtotal(), sumaCuotas, 10);

        assertThat(DataQualityIssues.getDataQualityIssues(List.of(factura), List.of())).isEmpty();
    }

    @Test
    void grupos21Y10_siguenUsandoLaToleranciaPorLinea() {
        List<CalculoFactura.LineaEntrada> lineas = List.of(
                new CalculoFactura.LineaEntrada(new BigDecimal("10.01"), 21),
                new CalculoFactura.LineaEntrada(new BigDecimal("10.01"), 10)
        );
        CalculoFactura.Resultado totales = CalculoFactura.calcular(lineas, true, null);
        BigDecimal sumaCuotas = totales.cuotas().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        InvoiceSnapshot factura = new InvoiceSnapshot(
                13L, "F-13", 1L, totales.subtotal(), totales.iva(), totales.subtotal(), sumaCuotas, 2);

        assertThat(DataQualityIssues.getDataQualityIssues(List.of(factura), List.of())).isEmpty();
    }

    @Test
    void diezLineas_unCentimoDeMasSobreLaToleranciaAcumulada_seMarca() {
        InvoiceSnapshot factura = factura(11L, "F-11", 1L, "100.10", "21.02", "100.10", "20.91", 10);

        assertThat(DataQualityIssues.getDataQualityIssues(List.of(factura), List.of()))
                .extracting(QualityIssue::tipo)
                .containsExactly(DataQualityIssues.LINEAS_NO_CUADRAN);
    }

    @Test
    void descuentoGlobalSoloEnCabecera_seMarcaAunqueElIvaQuepaEnLaTolerancia() {
        BigDecimal sumaLineas = new BigDecimal("100.10");
        BigDecimal ivaLineas = new BigDecimal("21.00");
        BigDecimal baseConDescuento = sumaLineas.multiply(new BigDecimal("0.90")).setScale(2, java.math.RoundingMode.HALF_UP);
        InvoiceSnapshot factura = new InvoiceSnapshot(12L, "F-12", 1L, baseConDescuento, ivaLineas, sumaLineas, ivaLineas, 10);

        List<QualityIssue> issues = DataQualityIssues.getDataQualityIssues(List.of(factura), List.of());

        assertThat(baseConDescuento).isEqualByComparingTo("90.09");
        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.tipo()).isEqualTo(DataQualityIssues.LINEAS_NO_CUADRAN);
            assertThat(issue.etiqueta()).contains("F-12");
        });
    }

    @Test
    void importesNegativosQueCuadran_noSeMarcan_yUnDescuadreSi() {
        InvoiceSnapshot abonoOk = factura(20L, "R-1", 1L, "-100.00", "-21.00", "-100.00", "-21.00", 1);
        InvoiceSnapshot abonoMal = factura(21L, "R-2", 1L, "-80.00", "-16.80", "-100.00", "-21.00", 1);
        InvoiceSnapshot ceros = factura(22L, "F-0", 1L, "0.00", "0.00", "0.00", "0.00", 1);

        assertThat(DataQualityIssues.getDataQualityIssues(List.of(abonoOk, ceros), List.of())).isEmpty();
        assertThat(DataQualityIssues.getDataQualityIssues(List.of(abonoMal), List.of()))
                .extracting(QualityIssue::tipo)
                .containsExactly(DataQualityIssues.LINEAS_NO_CUADRAN);
    }

    @Test
    void sinClienteYlineasDescuadradas_devuelveLasDos() {
        InvoiceSnapshot factura = factura(8L, "F-8", null, "10.00", "2.10", "0.00", "0.00", 1);

        List<QualityIssue> issues = DataQualityIssues.getDataQualityIssues(List.of(factura), List.of());

        assertThat(issues).extracting(QualityIssue::tipo)
                .containsExactly(DataQualityIssues.LINEAS_NO_CUADRAN, DataQualityIssues.SIN_CLIENTE);
        assertThat(issues).allSatisfy(issue -> assertThat(issue.ruta()).isEqualTo("/facturas/8"));
    }

    @Test
    void gastoSinFechaYsinCategoria_devuelveLasDos() {
        ExpenseSnapshot gasto = new ExpenseSnapshot(15L, null, "  ", "Papelería Norte");

        List<QualityIssue> issues = DataQualityIssues.getDataQualityIssues(List.of(), List.of(gasto));

        assertThat(issues).extracting(QualityIssue::tipo)
                .containsExactly(DataQualityIssues.SIN_FECHA, DataQualityIssues.SIN_CATEGORIA);
        assertThat(issues).allSatisfy(issue -> {
            assertThat(issue.ruta()).isEqualTo("/gastos/15");
            assertThat(issue.etiqueta()).startsWith("Gasto de Papelería Norte:");
        });
    }

    @Test
    void gastoSinProveedor_usaElIdEnLaEtiqueta() {
        ExpenseSnapshot gasto = new ExpenseSnapshot(3L, null, "MATERIAL", null);

        List<QualityIssue> issues = DataQualityIssues.getDataQualityIssues(null, List.of(gasto));

        assertThat(issues).singleElement().satisfies(issue ->
                assertThat(issue.etiqueta()).isEqualTo("Gasto #3: sin fecha"));
    }

    @Test
    void listasNulasOConNulos_noFalla() {
        assertThat(DataQualityIssues.getDataQualityIssues(null, null)).isEmpty();
        assertThat(DataQualityIssues.getDataQualityIssues(java.util.Arrays.asList((InvoiceSnapshot) null), java.util.Arrays.asList((ExpenseSnapshot) null)))
                .isEmpty();
    }

    @Test
    void textoContexto_singularYplural() {
        assertThat(DataQualityIssues.textoContexto(1, 1, 1)).isEqualTo("1 factura · 1 gasto · 1 incidencia");
        assertThat(DataQualityIssues.textoContexto(12, 8, 2)).isEqualTo("12 facturas · 8 gastos · 2 incidencias");
        assertThat(DataQualityIssues.textoContexto(0, 0, 0)).isEqualTo("0 facturas · 0 gastos · 0 incidencias");
    }

    private static InvoiceSnapshot factura(
            long id,
            String numero,
            Long clienteId,
            String subtotal,
            String iva,
            String sumaBase,
            String sumaIva,
            int lineas
    ) {
        return new InvoiceSnapshot(
                id,
                numero,
                clienteId,
                new BigDecimal(subtotal),
                new BigDecimal(iva),
                new BigDecimal(sumaBase),
                new BigDecimal(sumaIva),
                lineas
        );
    }

    private static ExpenseSnapshot gastoOk() {
        return new ExpenseSnapshot(9L, LocalDate.of(2026, 3, 2), "MATERIAL", "Ferretería");
    }
}
