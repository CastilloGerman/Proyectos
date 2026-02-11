package appgestion.service;

import appgestion.config.PlantillaConfig;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Genera PDFs de presupuestos y facturas con plantilla (empresa, logo opcional).
 * Equivalente al PDFGenerator de la app Python.
 */
public class PdfGeneratorService {

    /** Document como recurso para try-with-resources (Document ya implementa AutoCloseable). */
    private static final class CloseableDocument extends Document {
        CloseableDocument(Rectangle pageSize, float marginLeft, float marginRight, float marginTop, float marginBottom) {
            super(pageSize, marginLeft, marginRight, marginTop, marginBottom);
        }
    }

    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0x2c, 0x3e, 0x50));
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(0x2c, 0x3e, 0x50));
    private static final Font FONT_NORMAL = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(0x2c, 0x3e, 0x50));
    private static final Font FONT_EMPRESA = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(0x2c, 0x3e, 0x50));

    private final PlantillaConfig plantilla;

    public PdfGeneratorService() {
        this.plantilla = new ConfigService().getConfig();
    }

    public PdfGeneratorService(PlantillaConfig plantilla) {
        this.plantilla = plantilla != null ? plantilla : new ConfigService().getConfig();
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    /**
     * Genera un PDF del presupuesto y lo guarda en outputPath.
     * @return ruta del archivo generado
     */
    public Path generatePresupuestoPdf(PresupuestoService.PresupuestoDetalle presupuesto, Path outputPath) throws DocumentException, IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             CloseableDocument doc = new CloseableDocument(PageSize.A4, 72, 72, 72, 72)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();
                addCabeceraEmpresa(doc, "PRESUPUESTO #" + presupuesto.id);
                doc.add(new Paragraph("Cliente: " + nullToEmpty(presupuesto.clienteNombre), FONT_NORMAL));
                doc.add(new Paragraph("Teléfono: " + nullToEmpty(presupuesto.telefono), FONT_NORMAL));
                doc.add(new Paragraph("Email: " + nullToEmpty(presupuesto.email), FONT_NORMAL));
                doc.add(new Paragraph("Dirección: " + nullToEmpty(presupuesto.direccion), FONT_NORMAL));
                doc.add(Chunk.NEWLINE);
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100f);
                table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f});
                addCell(table, "Descripción", true);
                addCell(table, "Cant.", true);
                addCell(table, "Precio unit.", true);
                addCell(table, "Subtotal", true);
                List<PresupuestoService.PresupuestoItemDetalle> items = presupuesto.items;
                if (items != null) {
                    for (PresupuestoService.PresupuestoItemDetalle it : items) {
                        String desc = it.esTareaManual
                                ? nullToEmpty(it.tareaManual)
                                : nullToEmpty(it.materialNombre) + (it.unidadMedida != null && !it.unidadMedida.isEmpty() ? " (" + it.unidadMedida + ")" : "");
                        addCell(table, desc, false);
                        addCell(table, fmt(it.cantidad), false);
                        addCell(table, fmt(it.precioUnitario) + " €", false);
                        addCell(table, fmt(it.subtotal) + " €", false);
                    }
                }
                doc.add(table);
                doc.add(Chunk.NEWLINE);
                doc.add(new Paragraph("Subtotal: " + fmt(presupuesto.subtotal) + " €", FONT_NORMAL));
                if (presupuesto.ivaHabilitado) {
                    doc.add(new Paragraph("IVA (21%): " + fmt(presupuesto.iva) + " €", FONT_NORMAL));
                } else {
                    doc.add(new Paragraph("IVA: No incluido", FONT_NORMAL));
                }
                doc.add(new Paragraph("Total: " + fmt(presupuesto.total) + " €", FONT_HEADER));
        }
        return outputPath;
    }

    /**
     * Genera un PDF de la factura y lo guarda en outputPath.
     */
    public Path generateFacturaPdf(FacturaService.FacturaDetalle factura, Path outputPath) throws DocumentException, IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             CloseableDocument doc = new CloseableDocument(PageSize.A4, 72, 72, 72, 72)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();
                addCabeceraEmpresa(doc, "FACTURA " + nullToEmpty(factura.numeroFactura));
                doc.add(new Paragraph("Fecha: " + nullToEmpty(factura.fechaCreacion), FONT_NORMAL));
                doc.add(new Paragraph("Vencimiento: " + nullToEmpty(factura.fechaVencimiento), FONT_NORMAL));
                doc.add(new Paragraph("Método de pago: " + nullToEmpty(factura.metodoPago), FONT_NORMAL));
                doc.add(new Paragraph("Estado: " + nullToEmpty(factura.estadoPago), FONT_NORMAL));
                doc.add(Chunk.NEWLINE);
                doc.add(new Paragraph("Cliente: " + nullToEmpty(factura.clienteNombre), FONT_NORMAL));
                doc.add(new Paragraph("Teléfono: " + nullToEmpty(factura.telefono), FONT_NORMAL));
                doc.add(new Paragraph("Email: " + nullToEmpty(factura.email), FONT_NORMAL));
                doc.add(new Paragraph("Dirección: " + nullToEmpty(factura.direccion), FONT_NORMAL));
                doc.add(Chunk.NEWLINE);
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100f);
                table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f});
                addCell(table, "Descripción", true);
                addCell(table, "Cant.", true);
                addCell(table, "Precio unit.", true);
                addCell(table, "Subtotal", true);
                List<FacturaService.FacturaItemDetalle> items = factura.items;
                if (items != null) {
                    for (FacturaService.FacturaItemDetalle it : items) {
                        String desc = it.esTareaManual
                                ? nullToEmpty(it.tareaManual)
                                : nullToEmpty(it.materialNombre) + (it.unidadMedida != null && !it.unidadMedida.isEmpty() ? " (" + it.unidadMedida + ")" : "");
                        addCell(table, desc, false);
                        addCell(table, fmt(it.cantidad), false);
                        addCell(table, fmt(it.precioUnitario) + " €", false);
                        addCell(table, fmt(it.subtotal) + " €", false);
                    }
                }
                doc.add(table);
                doc.add(Chunk.NEWLINE);
                doc.add(new Paragraph("Subtotal: " + fmt(factura.subtotal) + " €", FONT_NORMAL));
                if (factura.ivaHabilitado) {
                    doc.add(new Paragraph("IVA (21%): " + fmt(factura.iva) + " €", FONT_NORMAL));
                } else {
                    doc.add(new Paragraph("IVA: No incluido", FONT_NORMAL));
                }
                doc.add(new Paragraph("Total: " + fmt(factura.total) + " €", FONT_HEADER));
                if (factura.notas != null && !factura.notas.isEmpty()) {
                    doc.add(Chunk.NEWLINE);
                    doc.add(new Paragraph("Notas: " + factura.notas, FONT_NORMAL));
                }
        }
        return outputPath;
    }

    /** Añade cabecera con logo (si está configurado) y nombre de empresa, luego el título del documento. */
    private void addCabeceraEmpresa(Document doc, String tituloDocumento) throws DocumentException, IOException {
        if (plantilla == null || plantilla.empresa == null) {
            Paragraph titulo = new Paragraph(tituloDocumento, FONT_TITLE);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            doc.add(titulo);
            return;
        }
        PlantillaConfig.Empresa e = plantilla.empresa;
        PlantillaConfig.Logo l = plantilla.logo != null ? plantilla.logo : new PlantillaConfig.Logo();

        PdfPTable cabecera = new PdfPTable(2);
        cabecera.setWidthPercentage(100f);
        cabecera.setWidths(new float[]{1.5f, 3f});
        cabecera.setSpacingAfter(15);

        // Celda izquierda: logo o texto
        if (l.usarLogo && l.rutaLogo != null && !l.rutaLogo.isEmpty() && Files.isRegularFile(Paths.get(l.rutaLogo))) {
            try {
                Image img = Image.getInstance(l.rutaLogo);
                img.scaleToFit(100, 50);
                PdfPCell cellLogo = new PdfPCell(img);
                cellLogo.setBorder(Rectangle.NO_BORDER);
                cellLogo.setPadding(0);
                cabecera.addCell(cellLogo);
            } catch (IOException | DocumentException ex) {
                addCell(cabecera, l.textoLogo != null && !l.textoLogo.isEmpty() ? l.textoLogo : e.nombre, true, true);
            }
        } else {
            String textoLogo = (l.textoLogo != null && !l.textoLogo.isEmpty()) ? l.textoLogo : e.nombre;
            addCell(cabecera, textoLogo, true, true);
        }

        // Celda derecha: datos empresa
        StringBuilder sb = new StringBuilder();
        if (e.nombre != null) sb.append(e.nombre).append("\n");
        if (e.direccion != null) sb.append(e.direccion).append("\n");
            if (e.codigoPostal != null || e.ciudad != null)
                sb.append(nullToEmpty(e.codigoPostal)).append(" ").append(nullToEmpty(e.ciudad)).append("\n");
        if (e.telefono != null) sb.append("T. ").append(e.telefono).append("\n");
        if (e.email != null) sb.append(e.email);
        PdfPCell cellEmpresa = new PdfPCell(new Phrase(sb.toString(), FONT_NORMAL));
        cellEmpresa.setBorder(Rectangle.NO_BORDER);
        cellEmpresa.setVerticalAlignment(Element.ALIGN_TOP);
        cabecera.addCell(cellEmpresa);

        doc.add(cabecera);

        Paragraph titulo = new Paragraph(tituloDocumento, FONT_TITLE);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(16);
        doc.add(titulo);
    }

    private static void addCell(PdfPTable table, String text, boolean bold, boolean noBorder) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold ? FONT_EMPRESA : FONT_NORMAL));
        cell.setPadding(5);
        if (noBorder) cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static void addCell(PdfPTable table, String text, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(text, header ? FONT_HEADER : FONT_NORMAL));
        cell.setPadding(5);
        if (header) {
            cell.setBackgroundColor(new Color(0xf8, 0xf9, 0xfa));
        }
        table.addCell(cell);
    }
}
