package com.appgestion.api.fiscal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Incidencias de calidad del resumen 303. No accede a base de datos: el servicio
 * le pasa instantáneas ya leídas y esta clase solo decide qué avisar.
 */
public final class DataQualityIssues {

    public static final String LINEAS_NO_CUADRAN = "LINEAS_NO_CUADRAN";
    public static final String SIN_CLIENTE = "SIN_CLIENTE";
    public static final String SIN_FECHA = "SIN_FECHA";
    public static final String SIN_CATEGORIA = "SIN_CATEGORIA";

    /**
     * Un céntimo por línea, acumulable. Con 10 líneas se admiten hasta 0,10 € de
     * diferencia en el IVA, no un tope único de 0,01 € para toda la factura.
     * La base no usa esta tolerancia: tiene que coincidir al céntimo.
     */
    private static final BigDecimal CENT_POR_LINEA = new BigDecimal("0.01");

    private DataQualityIssues() {
    }

    /**
     * @param subtotal          base de la cabecera ({@code facturas.subtotal})
     * @param iva               cuota de la cabecera ({@code facturas.iva}), no el total a pagar
     * @param sumaLineasSubtotal suma de {@code factura_items.subtotal}
     * @param sumaLineasIva     suma de {@code factura_items.cuota_iva}
     * @param numeroLineas      líneas de la factura; 0 si no hay ninguna
     */
    public record InvoiceSnapshot(
            long id,
            String numero,
            Long clienteId,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal sumaLineasSubtotal,
            BigDecimal sumaLineasIva,
            int numeroLineas
    ) {
    }

    public record ExpenseSnapshot(
            long id,
            LocalDate fecha,
            String categoria,
            String proveedor
    ) {
    }

    public record QualityIssue(
            String tipo,
            long id,
            String etiqueta,
            String ruta
    ) {
    }

    /**
     * Detecta facturas cuyas líneas no cuadran con la cabecera, facturas sin cliente
     * y gastos sin fecha o sin categoría.
     * <p>
     * La base (y una cabecera sin líneas) debe coincidir al céntimo. Un descuento
     * global aplicado solo en la cabecera, aunque sea de 0,01 €, cuenta como descuadre.
     * En el IVA la tolerancia es {@code 0,01 € × número de líneas}: cada cuota se
     * redondea por separado y el error se acumula. No es un margen fijo de 0,01 €.
     */
    public static List<QualityIssue> getDataQualityIssues(
            List<InvoiceSnapshot> invoices,
            List<ExpenseSnapshot> expenses
    ) {
        List<QualityIssue> issues = new ArrayList<>();
        if (invoices != null) {
            for (InvoiceSnapshot invoice : invoices) {
                if (invoice == null) {
                    continue;
                }
                if (lineasNoCuadran(invoice)) {
                    issues.add(new QualityIssue(
                            LINEAS_NO_CUADRAN,
                            invoice.id(),
                            "Factura " + referenciaFactura(invoice) + ": las líneas no cuadran con el total",
                            "/facturas/" + invoice.id()
                    ));
                }
                if (invoice.clienteId() == null) {
                    issues.add(new QualityIssue(
                            SIN_CLIENTE,
                            invoice.id(),
                            "Factura " + referenciaFactura(invoice) + ": sin cliente",
                            "/facturas/" + invoice.id()
                    ));
                }
            }
        }
        if (expenses != null) {
            for (ExpenseSnapshot expense : expenses) {
                if (expense == null) {
                    continue;
                }
                if (expense.fecha() == null) {
                    issues.add(new QualityIssue(
                            SIN_FECHA,
                            expense.id(),
                            etiquetaGasto(expense, "sin fecha"),
                            "/gastos/" + expense.id()
                    ));
                }
                if (expense.categoria() == null || expense.categoria().isBlank()) {
                    issues.add(new QualityIssue(
                            SIN_CATEGORIA,
                            expense.id(),
                            etiquetaGasto(expense, "sin categoría"),
                            "/gastos/" + expense.id()
                    ));
                }
            }
        }
        return List.copyOf(issues);
    }

    /** Texto del indicador "12 facturas · 8 gastos · 2 incidencias". */
    public static String textoContexto(long facturas, long gastos, int incidencias) {
        return etiquetaCantidad(facturas, "factura", "facturas")
                + " · "
                + etiquetaCantidad(gastos, "gasto", "gastos")
                + " · "
                + etiquetaCantidad(incidencias, "incidencia", "incidencias");
    }

    private static boolean lineasNoCuadran(InvoiceSnapshot invoice) {
        boolean baseOk = money(invoice.subtotal()).compareTo(money(invoice.sumaLineasSubtotal())) == 0;
        int lineas = Math.max(invoice.numeroLineas(), 0);
        BigDecimal toleranciaIva = lineas == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : CENT_POR_LINEA.multiply(BigDecimal.valueOf(lineas));
        boolean ivaOk = money(invoice.iva()).subtract(money(invoice.sumaLineasIva())).abs().compareTo(toleranciaIva) <= 0;
        return !baseOk || !ivaOk;
    }

    private static String referenciaFactura(InvoiceSnapshot invoice) {
        String numero = invoice.numero() == null ? "" : invoice.numero().trim();
        if (numero.isEmpty()) {
            return "#" + invoice.id();
        }
        return numero;
    }

    private static String etiquetaGasto(ExpenseSnapshot expense, String motivo) {
        String proveedor = expense.proveedor() == null ? "" : expense.proveedor().trim();
        if (proveedor.isEmpty()) {
            return "Gasto #" + expense.id() + ": " + motivo;
        }
        return "Gasto de " + proveedor + ": " + motivo;
    }

    private static String etiquetaCantidad(long cantidad, String singular, String plural) {
        return cantidad + " " + (cantidad == 1 ? singular : plural);
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
