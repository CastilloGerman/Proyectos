package appgestion.service;

import appgestion.config.PlantillaConfig;
import appgestion.util.StringUtils;
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
                doc.add(new Paragraph("Cliente: " + StringUtils.nullToEmpty(presupuesto.clienteNombre), FONT_NORMAL));
                doc.add(new Paragraph("Teléfono: " + StringUtils.nullToEmpty(presupuesto.telefono), FONT_NORMAL));
                doc.add(new Paragraph("Email: " + StringUtils.nullToEmpty(presupuesto.email), FONT_NORMAL));
                doc.add(new Paragraph("Dirección: " + StringUtils.nullToEmpty(presupuesto.direccion), FONT_NORMAL));
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
                                ? StringUtils.nullToEmpty(it.tareaManual)
                                : StringUtils.nullToEmpty(it.materialNombre) + (it.unidadMedida != null && !it.unidadMedida.isEmpty() ? " (" + it.unidadMedida + ")" : "");
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
     * Formato conforme al RD 1619/2012 (Reglamento de facturación español).
     */
    public Path generateFacturaPdf(FacturaService.FacturaDetalle factura, Path outputPath) throws DocumentException, IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             CloseableDocument doc = new CloseableDocument(PageSize.A4, 72, 72, 72, 72)) {
            PdfWriter.getInstance(doc, fos);
            doc.open();
            addCabeceraFacturaLegal(doc, factura);
            addDatosEmisorDestinatario(doc, factura);
            addDetalleFactura(doc, factura);
        }
        return outputPath;
    }

    /** Cabecera: título FACTURA + número + fecha expedición según RD 1619/2012 */
    private void addCabeceraFacturaLegal(Document doc, FacturaService.FacturaDetalle factura) throws DocumentException, IOException {
        if (plantilla != null && plantilla.empresa != null) {
            PlantillaConfig.Empresa e = plantilla.empresa;
            PlantillaConfig.Logo l = plantilla.logo != null ? plantilla.logo : new PlantillaConfig.Logo();
            PdfPTable cabecera = new PdfPTable(2);
            cabecera.setWidthPercentage(100f);
            cabecera.setWidths(new float[]{1.5f, 3f});
            cabecera.setSpacingAfter(15);
            if (l.usarLogo && l.rutaLogo != null && !l.rutaLogo.isEmpty() && Files.isRegularFile(Paths.get(l.rutaLogo))) {
                try {
                    Image img = Image.getInstance(l.rutaLogo);
                    img.scaleToFit(100, 50);
                    PdfPCell cellLogo = new PdfPCell(img);
                    cellLogo.setBorder(Rectangle.NO_BORDER);
                    cabecera.addCell(cellLogo);
                } catch (IOException | DocumentException ex) {
                    addCell(cabecera, l.textoLogo != null ? l.textoLogo : e.nombre, true, true);
                }
            } else {
                addCell(cabecera, l.textoLogo != null && !l.textoLogo.isEmpty() ? l.textoLogo : e.nombre, true, true);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.nullToEmpty(e.nombre)).append("\n");
            sb.append("CIF: ").append(StringUtils.nullToEmpty(e.cif)).append("\n");
            sb.append(StringUtils.nullToEmpty(e.direccion)).append("\n");
            sb.append(StringUtils.nullToEmpty(e.codigoPostal)).append(" ").append(StringUtils.nullToEmpty(e.ciudad)).append("\n");
            sb.append("T. ").append(StringUtils.nullToEmpty(e.telefono)).append(" | ").append(StringUtils.nullToEmpty(e.email));
            PdfPCell cellEmpresa = new PdfPCell(new Phrase(sb.toString(), FONT_NORMAL));
            cellEmpresa.setBorder(Rectangle.NO_BORDER);
            cellEmpresa.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cabecera.addCell(cellEmpresa);
            doc.add(cabecera);
        }
        PdfPTable titulo = new PdfPTable(2);
        titulo.setWidthPercentage(100f);
        titulo.setWidths(new float[]{3f, 2f});
        addCell(titulo, "FACTURA", true, true);
        StringBuilder sb = new StringBuilder();
        sb.append("Nº ").append(StringUtils.nullToEmpty(factura.numeroFactura)).append("\n");
        sb.append("Fecha expedición: ").append(StringUtils.nullToEmpty(factura.fechaCreacion)).append("\n");
        sb.append("Vencimiento: ").append(StringUtils.nullToEmpty(factura.fechaVencimiento)).append("\n");
        sb.append("Forma pago: ").append(StringUtils.nullToEmpty(factura.metodoPago));
        PdfPCell cellNum = new PdfPCell(new Phrase(sb.toString(), FONT_NORMAL));
        cellNum.setBorder(Rectangle.NO_BORDER);
        cellNum.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titulo.addCell(cellNum);
        doc.add(titulo);
        doc.add(Chunk.NEWLINE);
    }

    /** Datos emisor y destinatario (obligatorios RD 1619/2012) */
    private void addDatosEmisorDestinatario(Document doc, FacturaService.FacturaDetalle factura) throws DocumentException {
        if (plantilla == null || plantilla.empresa == null) return;
        PlantillaConfig.Empresa e = plantilla.empresa;
        PdfPTable bloques = new PdfPTable(2);
        bloques.setWidthPercentage(100f);
        bloques.setWidths(new float[]{1f, 1f});
        bloques.setSpacingAfter(15);
        String emisor = "EMISOR: " + StringUtils.nullToEmpty(e.nombre) + "\nCIF: " + StringUtils.nullToEmpty(e.cif) + "\n"
                + StringUtils.nullToEmpty(e.direccion) + "\n" + StringUtils.nullToEmpty(e.codigoPostal) + " " + StringUtils.nullToEmpty(e.ciudad)
                + "\nT. " + StringUtils.nullToEmpty(e.telefono) + "\n" + StringUtils.nullToEmpty(e.email);
        String destinatario = "CLIENTE: " + StringUtils.nullToEmpty(factura.clienteNombre) + "\n"
                + (factura.clienteDni != null && !factura.clienteDni.isEmpty() ? "NIF/CIF: " + factura.clienteDni + "\n" : "")
                + StringUtils.nullToEmpty(factura.direccion) + "\n"
                + (factura.telefono != null && !factura.telefono.isEmpty() ? "T. " + factura.telefono + "\n" : "")
                + StringUtils.nullToEmpty(factura.email);
        PdfPCell cell1 = new PdfPCell(new Phrase(emisor, FONT_NORMAL));
        cell1.setBorder(Rectangle.NO_BORDER);
        cell1.setPadding(8);
        cell1.setBackgroundColor(new Color(0xf8, 0xf9, 0xfa));
        PdfPCell cell2 = new PdfPCell(new Phrase(destinatario, FONT_NORMAL));
        cell2.setBorder(Rectangle.NO_BORDER);
        cell2.setPadding(8);
        cell2.setBackgroundColor(new Color(0xf8, 0xf9, 0xfa));
        bloques.addCell(cell1);
        bloques.addCell(cell2);
        doc.add(bloques);
    }

    /** Tabla de líneas y totales (RD 1619/2012: descripción, precio unit., IVA, total) */
    private void addDetalleFactura(Document doc, FacturaService.FacturaDetalle factura) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{3f, 1f, 1.5f, 1f, 1.5f});
        addCell(table, "Descripción de la operación", true);
        addCell(table, "Cant.", true);
        addCell(table, "Precio unit. (€)", true);
        addCell(table, "IVA %", true);
        addCell(table, "Importe (€)", true);
        List<FacturaService.FacturaItemDetalle> items = factura.items;
        if (items != null) {
            for (FacturaService.FacturaItemDetalle it : items) {
                String desc = it.esTareaManual
                        ? StringUtils.nullToEmpty(it.tareaManual)
                        : StringUtils.nullToEmpty(it.materialNombre) + (it.unidadMedida != null && !it.unidadMedida.isEmpty() ? " (" + it.unidadMedida + ")" : "");
                addCell(table, desc, false);
                addCell(table, fmt(it.cantidad), false);
                addCell(table, fmt(it.precioUnitario), false);
                addCell(table, factura.ivaHabilitado ? "21" : "0", false);
                addCell(table, fmt(it.subtotal), false);
            }
        }
        doc.add(table);
        doc.add(Chunk.NEWLINE);
        PdfPTable totales = new PdfPTable(2);
        totales.setWidthPercentage(50f);
        totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totales.setWidths(new float[]{2f, 1.5f});
        addCell(totales, "Base imponible:", false);
        addCell(totales, fmt(factura.subtotal) + " €", false);
        if (factura.ivaHabilitado) {
            addCell(totales, "IVA (21%):", false);
            addCell(totales, fmt(factura.iva) + " €", false);
        }
        addCell(totales, "Total:", true);
        addCell(totales, fmt(factura.total) + " €", true);
        doc.add(totales);
        if (factura.notas != null && !factura.notas.isEmpty()) {
            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("Notas: " + factura.notas, FONT_NORMAL));
        }
        doc.add(Chunk.NEWLINE);
        PdfPCell legal = new PdfPCell(new Phrase("Factura conforme al Reglamento de facturación (RD 1619/2012).", new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY)));
        legal.setBorder(Rectangle.NO_BORDER);
        legal.setPadding(5);
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100f);
        footer.addCell(legal);
        doc.add(footer);
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
                sb.append(StringUtils.nullToEmpty(e.codigoPostal)).append(" ").append(StringUtils.nullToEmpty(e.ciudad)).append("\n");
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

    private static void addCell(PdfPTable table, String text, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(text, header ? FONT_HEADER : FONT_NORMAL));
        cell.setPadding(5);
        if (header) {
            cell.setBackgroundColor(new Color(0xf8, 0xf9, 0xfa));
        }
        table.addCell(cell);
    }
}
