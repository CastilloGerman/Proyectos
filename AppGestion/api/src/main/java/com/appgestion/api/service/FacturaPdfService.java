package com.appgestion.api.service;

import com.appgestion.api.domain.entity.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Genera PDF de facturas cumpliendo requisitos del Reglamento de facturación español
 * (RD 1619/2012): número, fecha, datos emisor/receptor, descripción, base imponible, IVA, total.
 */
@Service
public class FacturaPdfService {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color HEADER_BG = new Color(45, 55, 72);
    private static final Color ROW_ALT = new Color(248, 250, 252);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final float[] TABLE_WIDTHS = {3f, 1.2f, 1.5f, 1.5f, 1.5f};

    private final EmpresaService empresaService;

    public FacturaPdfService(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    public byte[] generarPdf(Factura factura, Long usuarioId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Document document = new Document(PageSize.A4, 40, 40, 50, 50)) {
            PdfWriter.getInstance(document, baos);
            document.open();
            agregarContenido(document, factura, usuarioId);
            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error al generar PDF de factura", e);
        }
    }

    private void agregarContenido(Document document, Factura factura, Long usuarioId) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        Empresa empresa = empresaService.getEmpresaOrNull(usuarioId);
        Cliente cliente = factura.getCliente();

        // Cabecera: FACTURA + número
        Paragraph title = new Paragraph("FACTURA " + (factura.getNumeroFactura() != null ? factura.getNumeroFactura() : ""), titleFont);
        title.setSpacingAfter(4);
        document.add(title);

        // Datos del emisor (obligatorio RD 1619/2012)
        if (empresa != null) {
            StringBuilder emisor = new StringBuilder();
            if (empresa.getNombre() != null && !empresa.getNombre().isBlank()) emisor.append(empresa.getNombre()).append("\n");
            if (empresa.getDireccion() != null && !empresa.getDireccion().isBlank()) emisor.append(empresa.getDireccion()).append("\n");
            if (empresa.getNif() != null && !empresa.getNif().isBlank()) emisor.append("NIF/CIF: ").append(empresa.getNif()).append("\n");
            if (empresa.getTelefono() != null && !empresa.getTelefono().isBlank()) emisor.append("Tel: ").append(empresa.getTelefono()).append("\n");
            if (empresa.getEmail() != null && !empresa.getEmail().isBlank()) emisor.append(empresa.getEmail());
            if (emisor.length() > 0) {
                Paragraph pEmisor = new Paragraph("EMISOR:\n" + emisor.toString().trim(), smallFont);
                pEmisor.setSpacingAfter(8);
                document.add(pEmisor);
            }
        }

        // Datos del receptor (obligatorio)
        StringBuilder receptor = new StringBuilder();
        receptor.append("CLIENTE: ").append(cliente != null ? cliente.getNombre() : "").append("\n");
        if (cliente != null) {
            if (cliente.getDireccion() != null && !cliente.getDireccion().isBlank()) receptor.append(cliente.getDireccion()).append("\n");
            if (cliente.getDni() != null && !cliente.getDni().isBlank()) receptor.append("NIF/CIF: ").append(cliente.getDni()).append("\n");
            if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) receptor.append(cliente.getEmail());
        }
        Paragraph pReceptor = new Paragraph(receptor.toString().trim(), smallFont);
        pReceptor.setSpacingAfter(12);
        document.add(pReceptor);

        // Fecha de expedición (obligatorio)
        String fechaExp = factura.getFechaCreacion() != null
                ? factura.getFechaCreacion().format(DATETIME_FORMAT) : "";
        Paragraph info = new Paragraph("Fecha de expedición: " + fechaExp, smallFont);
        info.setSpacingAfter(15);
        document.add(info);

        // Tabla de líneas (descripción, cantidad, precio unitario, IVA, subtotal)
        List<FacturaItem> items = factura.getItems();
        if (!items.isEmpty()) {
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(TABLE_WIDTHS);
            table.setSpacingBefore(10);
            table.setSpacingAfter(15);
            addTableHeader(table, headerFont);
            int rowNum = 0;
            for (FacturaItem item : items) {
                addTableRow(table, item, cellFont, factura.getIvaHabilitado(), rowNum % 2 == 1);
                rowNum++;
            }
            document.add(table);
        }

        // Totales (base imponible, IVA, total - obligatorios)
        PdfPTable totalesTable = crearTablaTotales(factura, cellFont);
        document.add(totalesTable);

        // Notas al pie (configurables)
        if (empresa != null && empresa.getNotasPieFactura() != null && !empresa.getNotasPieFactura().isBlank()) {
            Paragraph notas = new Paragraph(empresa.getNotasPieFactura(), smallFont);
            notas.setSpacingBefore(20);
            document.add(notas);
        }
        if (factura.getNotas() != null && !factura.getNotas().isBlank()) {
            Paragraph notasFactura = new Paragraph("Notas: " + factura.getNotas(), smallFont);
            notasFactura.setSpacingBefore(8);
            document.add(notasFactura);
        }
    }

    private void addTableHeader(PdfPTable table, Font font) {
        String[] headers = {"Descripción", "Cantidad", "P. Unit.", "IVA", "Importe"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(8);
            cell.setBorderColor(BORDER);
            cell.setBorderWidth(0.5f);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, FacturaItem item, Font font, boolean ivaHabilitado, boolean altRow) {
        String descripcion = Boolean.TRUE.equals(item.getEsTareaManual())
                ? (item.getTareaManual() != null ? item.getTareaManual() : "")
                : (item.getMaterial() != null ? item.getMaterial().getNombre() : "");
        double cantidad = Optional.ofNullable(item.getCantidad()).orElse(0.0);
        double precioUnit = Optional.ofNullable(item.getPrecioUnitario()).orElse(0.0);
        double subtotal = Optional.ofNullable(item.getSubtotal()).orElse(cantidad * precioUnit);
        String ivaStr = ivaHabilitado && Boolean.TRUE.equals(item.getAplicaIva()) ? "21%" : "0%";

        PdfPCell[] cells = {
                new PdfPCell(new Phrase(descripcion, font)),
                new PdfPCell(new Phrase(String.format("%.2f", cantidad), font)),
                new PdfPCell(new Phrase(String.format("%.2f €", precioUnit), font)),
                new PdfPCell(new Phrase(ivaStr, font)),
                new PdfPCell(new Phrase(String.format("%.2f €", subtotal), font))
        };
        for (PdfPCell cell : cells) {
            if (altRow) cell.setBackgroundColor(ROW_ALT);
            cell.setPadding(6);
            cell.setBorderColor(BORDER);
            cell.setBorderWidth(0.5f);
            table.addCell(cell);
        }
    }

    private PdfPTable crearTablaTotales(Factura factura, Font cellFont) {
        PdfPTable totalesTable = new PdfPTable(2);
        totalesTable.setWidthPercentage(40);
        totalesTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalesTable.setSpacingBefore(10);
        totalesTable.getDefaultCell().setBorderColor(BORDER);
        totalesTable.getDefaultCell().setPadding(6);
        totalesTable.getDefaultCell().setBorderWidth(0.5f);

        double subtotal = Optional.ofNullable(factura.getSubtotal()).orElse(0.0);
        double iva = Optional.ofNullable(factura.getIva()).orElse(0.0);
        double total = Optional.ofNullable(factura.getTotal()).orElse(0.0);

        totalesTable.addCell(new Phrase("Base imponible:", cellFont));
        totalesTable.addCell(new Phrase(String.format("%.2f €", subtotal), cellFont));
        if (Boolean.TRUE.equals(factura.getIvaHabilitado()) && iva > 0) {
            totalesTable.addCell(new Phrase("IVA (21%):", cellFont));
            totalesTable.addCell(new Phrase(String.format("%.2f €", iva), cellFont));
        }
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        totalesTable.addCell(new Phrase("TOTAL:", boldFont));
        totalesTable.addCell(new Phrase(String.format("%.2f €", total), boldFont));

        return totalesTable;
    }
}
