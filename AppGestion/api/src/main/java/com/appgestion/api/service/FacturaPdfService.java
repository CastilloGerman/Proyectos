package com.appgestion.api.service;

import com.appgestion.api.constant.TaxConstants;
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

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private void agregarContenido(Document document, Factura factura, Long usuarioId) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.DARK_GRAY);

        Empresa empresa = empresaService.getEmpresaOrNull(usuarioId);
        Cliente cliente = factura.getCliente();

        // Cabecera: FACTURA + número (RD 1619/2012 - número y serie)
        Paragraph title = new Paragraph("FACTURA " + (factura.getNumeroFactura() != null ? factura.getNumeroFactura() : ""), titleFont);
        title.setSpacingAfter(20);
        document.add(title);

        // Tabla de dos columnas: Emisor (izq) y Cliente/Destinatario (der) - RD 1619/2012
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1f, 1f});
        headerTable.setSpacingAfter(15);

        // Columna emisor
        PdfPCell cellEmisor = new PdfPCell();
        cellEmisor.setBorder(Rectangle.NO_BORDER);
        cellEmisor.setPadding(0);
        StringBuilder emisor = new StringBuilder();
        if (empresa != null) {
            if (empresa.getNombre() != null && !empresa.getNombre().isBlank()) emisor.append(empresa.getNombre()).append("\n");
            if (empresa.getDireccion() != null && !empresa.getDireccion().isBlank()) emisor.append(empresa.getDireccion()).append("\n");
            String cpProvPais = buildCpProvinciaPais(empresa.getCodigoPostal(), empresa.getProvincia(), empresa.getPais());
            if (!cpProvPais.isBlank()) emisor.append(cpProvPais).append("\n");
            if (empresa.getNif() != null && !empresa.getNif().isBlank()) emisor.append("NIF/CIF: ").append(empresa.getNif()).append("\n");
            if (empresa.getTelefono() != null && !empresa.getTelefono().isBlank()) emisor.append("Tel: ").append(empresa.getTelefono()).append("\n");
            if (empresa.getEmail() != null && !empresa.getEmail().isBlank()) emisor.append(empresa.getEmail());
        }
        if (emisor.length() > 0) {
            Paragraph pEmisor = new Paragraph("EMISOR:\n" + emisor.toString().trim(), smallFont);
            cellEmisor.addElement(pEmisor);
        }
        headerTable.addCell(cellEmisor);

        // Columna destinatario
        PdfPCell cellReceptor = new PdfPCell();
        cellReceptor.setBorder(Rectangle.NO_BORDER);
        cellReceptor.setPadding(0);
        StringBuilder receptor = new StringBuilder();
        receptor.append("DESTINATARIO:\n");
        receptor.append(cliente != null ? cliente.getNombre() : "").append("\n");
        if (cliente != null) {
            if (cliente.getDireccion() != null && !cliente.getDireccion().isBlank()) receptor.append(cliente.getDireccion()).append("\n");
            String cpProvPais = buildCpProvinciaPais(cliente.getCodigoPostal(), cliente.getProvincia(), cliente.getPais());
            if (!cpProvPais.isBlank()) receptor.append(cpProvPais).append("\n");
            if (cliente.getDni() != null && !cliente.getDni().isBlank()) receptor.append("NIF/CIF: ").append(cliente.getDni()).append("\n");
            if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) receptor.append(cliente.getEmail());
        }
        Paragraph pReceptor = new Paragraph(receptor.toString().trim(), smallFont);
        cellReceptor.addElement(pReceptor);
        headerTable.addCell(cellReceptor);
        document.add(headerTable);

        // Datos de factura (obligatorios RD 1619/2012)
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(60);
        infoTable.setWidths(new float[]{1.5f, 2f});
        infoTable.setSpacingAfter(15);
        infoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        infoTable.getDefaultCell().setPadding(2);

        String fechaExp = factura.getFechaExpedicion() != null ? factura.getFechaExpedicion().format(DATE_FORMAT) : "";
        addInfoRow(infoTable, "Fecha de expedición:", fechaExp, labelFont, smallFont);
        if (factura.getFechaOperacion() != null && !factura.getFechaOperacion().equals(factura.getFechaExpedicion())) {
            addInfoRow(infoTable, "Fecha de operación:", factura.getFechaOperacion().format(DATE_FORMAT), labelFont, smallFont);
        }
        if (factura.getFechaVencimiento() != null) {
            addInfoRow(infoTable, "Fecha de vencimiento:", factura.getFechaVencimiento().format(DATE_FORMAT), labelFont, smallFont);
        }
        String regimen = factura.getRegimenFiscal() != null ? factura.getRegimenFiscal() : "";
        if (!regimen.isBlank()) addInfoRow(infoTable, "Régimen fiscal:", regimen, labelFont, smallFont);
        String metodoPago = factura.getMetodoPago() != null ? factura.getMetodoPago() : "";
        if (!metodoPago.isBlank()) addInfoRow(infoTable, "Forma de pago:", metodoPago, labelFont, smallFont);
        String condicionesPago = factura.getCondicionesPago() != null ? factura.getCondicionesPago() : "";
        if (!condicionesPago.isBlank()) addInfoRow(infoTable, "Condiciones de pago:", condicionesPago, labelFont, smallFont);
        addInfoRow(infoTable, "Moneda:", factura.getMoneda() != null ? factura.getMoneda() : "EUR", labelFont, smallFont);
        addInfoRow(infoTable, "Estado:", factura.getEstadoPago() != null ? factura.getEstadoPago() : "", labelFont, smallFont);
        document.add(infoTable);

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

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, labelFont));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setPadding(2);
        table.addCell(cellLabel);
        PdfPCell cellValue = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        cellValue.setBorder(Rectangle.NO_BORDER);
        cellValue.setPadding(2);
        table.addCell(cellValue);
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
        String ivaStr = ivaHabilitado && Boolean.TRUE.equals(item.getAplicaIva()) ? TaxConstants.IVA_PERCENT_LABEL : "0%";

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
            totalesTable.addCell(new Phrase("IVA (" + TaxConstants.IVA_PERCENT_LABEL + "):", cellFont));
            totalesTable.addCell(new Phrase(String.format("%.2f €", iva), cellFont));
        }
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
        totalesTable.addCell(new Phrase("TOTAL (" + (factura.getMoneda() != null ? factura.getMoneda() : "EUR") + "):", boldFont));
        totalesTable.addCell(new Phrase(String.format("%.2f €", total), boldFont));

        return totalesTable;
    }

    private static String buildCpProvinciaPais(String cp, String provincia, String pais) {
        StringBuilder sb = new StringBuilder();
        if (cp != null && !cp.isBlank()) sb.append(cp);
        if (provincia != null && !provincia.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(provincia);
        }
        if (pais != null && !pais.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pais);
        }
        return sb.toString();
    }
}
