package com.appgestion.api.service;

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
    private static final Color HEADER_BG = new Color(45, 55, 72);
    private static final Color ROW_ALT = new Color(248, 250, 252);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final float[] TABLE_WIDTHS = {3f, 1.5f, 1.5f, 1.5f};

    public byte[] generarPdf(Presupuesto presupuesto) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Document document = new Document(PageSize.A4, 40, 40, 50, 50)) {
            PdfWriter.getInstance(document, baos);
            document.open();
            agregarContenido(document, presupuesto);
            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error al generar PDF", e);
        }
    }

    private void agregarContenido(Document document, Presupuesto presupuesto) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        Paragraph title = new Paragraph("PRESUPUESTO", titleFont);
        title.setSpacingAfter(4);
        document.add(title);

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
            totalesTable.addCell(new Phrase("IVA (21%):", cellFont));
            totalesTable.addCell(new Phrase(String.format("%.2f €", iva), cellFont));
        }
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        totalesTable.addCell(new Phrase("TOTAL:", boldFont));
        totalesTable.addCell(new Phrase(String.format("%.2f €", total), boldFont));

        return totalesTable;
    }
}
