package com.appgestion.api.service;

import com.appgestion.api.dto.response.Modelo303ResumenResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * PDF informativo del resumen orientativo Modelo 303.
 */
@Service
public class FiscalPdfService {

    private static final DecimalFormat EUR = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("es", "ES")));

    public byte[] generarPdfModelo303(Modelo303ResumenResponse r) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             Document document = new Document(PageSize.A4, 40, 40, 50, 50)) {
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 58, 138));
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            Paragraph title = new Paragraph("Resumen orientativo — Modelo 303 (IVA)", titleFont);
            title.setSpacingAfter(12);
            document.add(title);

            document.add(new Paragraph(
                    "Período: " + r.fechaDesde() + " — " + r.fechaHasta() + "  |  Año " + r.anio() + ", trimestre " + r.trimestre(),
                    bodyFont
            ));
            document.add(new Paragraph("Criterio: " + r.criterio() + (r.soloFacturasPagadas() ? " (solo facturas pagadas)" : ""), bodyFont));
            document.add(new Paragraph("Facturas incluidas: " + r.numeroFacturas(), bodyFont));
            document.add(new Paragraph(" ", bodyFont));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.2f, 1.3f});

            addHeaderRow(table, "Concepto", "Importe (€)", headFont);
            addBodyRow(table, "Base imponible total (ventas)", eur(r.baseImponibleTotal()), bodyFont);
            addBodyRow(table, "IVA repercutido", eur(r.ivaRepercutido()), bodyFont);
            addBodyRow(table, "IVA soportado (compras)", eur(r.ivaSoportado()), bodyFont);
            String resLabel = r.resultadoEsIngreso() ? "Resultado (a ingresar, orientativo)" : "Resultado (compensación / a favor, orientativo)";
            addBodyRow(table, resLabel, eur(r.resultadoIva().abs()), bodyFont);

            document.add(table);

            document.add(new Paragraph(" ", smallFont));
            document.add(new Paragraph("Nota IVA soportado: " + r.ivaSoportadoNota(), smallFont));

            for (String adv : r.advertencias()) {
                document.add(new Paragraph("• " + adv, smallFont));
            }

            document.add(new Paragraph(" ", smallFont));
            document.add(new Paragraph(r.avisoLegal(), smallFont));

            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error al generar PDF fiscal", e);
        }
    }

    private static void addHeaderRow(PdfPTable table, String a, String b, Font font) throws DocumentException {
        com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(new Phrase(a, font));
        c1.setBackgroundColor(new Color(30, 58, 138));
        c1.setPadding(8);
        com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(new Phrase(b, font));
        c2.setBackgroundColor(new Color(30, 58, 138));
        c2.setPadding(8);
        table.addCell(c1);
        table.addCell(c2);
    }

    private static void addBodyRow(PdfPTable table, String label, String value, Font font) {
        com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(new Phrase(label, font));
        c1.setPadding(6);
        com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(new Phrase(value, font));
        c2.setPadding(6);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(c1);
        table.addCell(c2);
    }

    private static String eur(java.math.BigDecimal v) {
        if (v == null) {
            return "0,00";
        }
        return EUR.format(v);
    }
}
