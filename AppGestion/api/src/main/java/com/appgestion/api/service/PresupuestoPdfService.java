package com.appgestion.api.service;

import com.appgestion.api.constant.TaxConstants;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.PresupuestoItem;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class PresupuestoPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final EmpresaService empresaService;
    private final PresupuestoCondicionesService presupuestoCondicionesService;
    public PresupuestoPdfService(
            EmpresaService empresaService,
            PresupuestoCondicionesService presupuestoCondicionesService) {
        this.empresaService = empresaService;
        this.presupuestoCondicionesService = presupuestoCondicionesService;
    }
    private static final Color HEADER_BG = new Color(45, 55, 72);
    private static final Color ROW_ALT = new Color(248, 250, 252);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final float[] TABLE_WIDTHS = {3f, 1.5f, 1.5f, 1.5f};

    public byte[] generarPdf(Presupuesto presupuesto, Long usuarioId) {
        return generarPdfInterno(presupuesto, usuarioId, null);
    }

    /** Vista previa del editor: {@code notasPieOverride} no nulo sustituye las notas al pie de empresa. */
    public byte[] generarVistaPrevia(Presupuesto presupuesto, Long usuarioId, String notasPieOverride) {
        return generarPdfInterno(presupuesto, usuarioId, notasPieOverride);
    }

    private byte[] generarPdfInterno(Presupuesto presupuesto, Long usuarioId, String notasPiePresupuestoOverride) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Document document = new Document(PageSize.A4, 40, 40, 50, 50)) {
            PdfWriter.getInstance(document, baos);
            document.open();
            agregarContenido(document, presupuesto, usuarioId, notasPiePresupuestoOverride);
            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    private void agregarContenido(Document document, Presupuesto presupuesto, Long usuarioId, String notasPiePresupuestoOverride)
            throws DocumentException, IOException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        Empresa empresa = usuarioId != null ? empresaService.getEmpresaOrNull(usuarioId) : null;

        byte[] logo = empresa != null ? empresa.getLogoImagen() : null;
        PdfLogoHelper.agregarTituloConLogoOpcional(document, logo, "PRESUPUESTO", titleFont);

        if (empresa != null && (empresa.getNombre() != null && !empresa.getNombre().isBlank()
                || empresa.getDireccion() != null || empresa.getNif() != null)) {
            StringBuilder emisor = new StringBuilder();
            if (empresa.getNombre() != null && !empresa.getNombre().isBlank()) emisor.append(empresa.getNombre()).append("\n");
            if (empresa.getDireccion() != null && !empresa.getDireccion().isBlank()) emisor.append(empresa.getDireccion()).append("\n");
            if (empresa.getNif() != null && !empresa.getNif().isBlank()) emisor.append("NIF/CIF: ").append(empresa.getNif());
            if (emisor.length() > 0) {
                Paragraph pEmisor = new Paragraph(emisor.toString().trim(), smallFont);
                pEmisor.setSpacingAfter(8);
                document.add(pEmisor);
            }
        }

        String clienteNombre = presupuesto.getCliente() != null ? presupuesto.getCliente().getNombre() : "";
        String fecha = presupuesto.getFechaCreacion() != null
                ? presupuesto.getFechaCreacion().format(DATE_FORMAT) : "";
        Paragraph info = new Paragraph(
                "Cliente: " + clienteNombre + "  |  Fecha: " + fecha,
                smallFont
        );
        info.setSpacingAfter(20);
        document.add(info);

        List<PresupuestoItem> itemsVisibles = presupuesto.getItems().stream()
                .filter(item -> Optional.ofNullable(item.getVisiblePdf()).orElse(true))
                .toList();

        if (!itemsVisibles.isEmpty()) {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(TABLE_WIDTHS);
            table.setSpacingBefore(10);
            table.setSpacingAfter(15);
            addTableHeader(table, headerFont);
            int rowNum = 0;
            for (PresupuestoItem item : itemsVisibles) {
                addTableRow(table, item, cellFont, rowNum % 2 == 1);
                rowNum++;
            }
            document.add(table);
        }

        PdfPTable totalesTable = crearTablaTotales(presupuesto, cellFont);
        document.add(totalesTable);

        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY);
        var claves = presupuestoCondicionesService.desdeJson(presupuesto.getCondicionesActivasJson());
        var textosCond = presupuestoCondicionesService.textosPdfEnOrden(claves);
        if (!textosCond.isEmpty()) {
            Paragraph secCond = new Paragraph("Condiciones", sectionFont);
            secCond.setSpacingBefore(16);
            secCond.setSpacingAfter(6);
            document.add(secCond);
            for (String t : textosCond) {
                Paragraph cl = new Paragraph("\u2022 " + t, smallFont);
                cl.setSpacingBefore(2);
                document.add(cl);
            }
        }
        String nota = presupuesto.getNotaAdicional();
        if (nota != null && !nota.isBlank()) {
            Paragraph secNota = new Paragraph("Notas", sectionFont);
            secNota.setSpacingBefore(textosCond.isEmpty() ? 16 : 12);
            secNota.setSpacingAfter(6);
            document.add(secNota);
            for (String linea : nota.split("\\r?\\n")) {
                if (!linea.isBlank()) {
                    Paragraph p = new Paragraph(linea.trim(), smallFont);
                    p.setSpacingBefore(2);
                    document.add(p);
                }
            }
        }

        String piePresu;
        if (notasPiePresupuestoOverride != null) {
            piePresu = notasPiePresupuestoOverride.isBlank() ? null : notasPiePresupuestoOverride;
        } else if (empresa != null && empresa.getNotasPiePresupuesto() != null && !empresa.getNotasPiePresupuesto().isBlank()) {
            piePresu = empresa.getNotasPiePresupuesto();
        } else {
            piePresu = null;
        }
        if (piePresu != null) {
            Paragraph notas = new Paragraph(piePresu, smallFont);
            notas.setSpacingBefore(20);
            document.add(notas);
        }
    }

    private void addTableHeader(PdfPTable table, Font font) {
        String[] headers = {"Descripción", "Cantidad", "P. Unit.", "Subtotal"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(8);
            cell.setBorderColor(BORDER);
            cell.setBorderWidth(0.5f);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, PresupuestoItem item, Font font, boolean altRow) {
        String descripcion = item.getEsTareaManual() != null && item.getEsTareaManual()
                ? (item.getTareaManual() != null ? item.getTareaManual() : "")
                : (item.getMaterial() != null ? item.getMaterial().getNombre() : "");
        double cantidad = Optional.ofNullable(item.getCantidad()).orElse(0.0);
        double precioUnit = Optional.ofNullable(item.getPrecioUnitario()).orElse(0.0);
        double subtotal = Optional.ofNullable(item.getSubtotal()).orElse(cantidad * precioUnit);

        PdfPCell[] cells = {
                new PdfPCell(new Phrase(descripcion, font)),
                new PdfPCell(new Phrase(String.format("%.2f", cantidad), font)),
                new PdfPCell(new Phrase(String.format("%.2f €", precioUnit), font)),
                new PdfPCell(new Phrase(String.format("%.2f €", subtotal), font))
        };
        for (PdfPCell cell : cells) {
            if (altRow) {
                cell.setBackgroundColor(ROW_ALT);
            }
            cell.setPadding(6);
            cell.setBorderColor(BORDER);
            cell.setBorderWidth(0.5f);
            table.addCell(cell);
        }
    }

    private PdfPTable crearTablaTotales(Presupuesto presupuesto, Font cellFont) {
        PdfPTable totalesTable = new PdfPTable(2);
        totalesTable.setWidthPercentage(40);
        totalesTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalesTable.setSpacingBefore(10);
        totalesTable.getDefaultCell().setBorderColor(BORDER);
        totalesTable.getDefaultCell().setPadding(6);
        totalesTable.getDefaultCell().setBorderWidth(0.5f);

        double subtotal = Optional.ofNullable(presupuesto.getSubtotal()).orElse(0.0);
        double iva = Optional.ofNullable(presupuesto.getIva()).orElse(0.0);
        double total = Optional.ofNullable(presupuesto.getTotal()).orElse(0.0);

        totalesTable.addCell(new Phrase("Subtotal:", cellFont));
        totalesTable.addCell(new Phrase(String.format("%.2f €", subtotal), cellFont));
        if (Boolean.TRUE.equals(presupuesto.getIvaHabilitado()) && iva > 0) {
            totalesTable.addCell(new Phrase("IVA (" + TaxConstants.IVA_PERCENT_LABEL + "):", cellFont));
            totalesTable.addCell(new Phrase(String.format("%.2f €", iva), cellFont));
        }
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        totalesTable.addCell(new Phrase("TOTAL:", boldFont));
        totalesTable.addCell(new Phrase(String.format("%.2f €", total), boldFont));

        if (Boolean.TRUE.equals(presupuesto.getTieneAnticipo()) && presupuesto.getImporteAnticipo() != null) {
            double ant = presupuesto.getImporteAnticipo().doubleValue();
            if (ant > 0) {
                totalesTable.addCell(new Phrase("Anticipo / seña (IVA incl.):", cellFont));
                totalesTable.addCell(new Phrase(String.format("- %.2f €", ant), cellFont));
                double pendiente = Math.max(0.0, total - ant);
                Font pendFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0x15, 0x65, 0xc0));
                totalesTable.addCell(new Phrase("Pendiente a cobrar:", pendFont));
                totalesTable.addCell(new Phrase(String.format("%.2f €", pendiente), pendFont));
                if (Boolean.TRUE.equals(presupuesto.getAnticipoFacturado())) {
                    totalesTable.addCell(new Phrase("Factura de anticipo emitida (restante por factura final).", cellFont));
                    totalesTable.addCell(new Phrase("", cellFont));
                }
            }
        }

        return totalesTable;
    }
}
