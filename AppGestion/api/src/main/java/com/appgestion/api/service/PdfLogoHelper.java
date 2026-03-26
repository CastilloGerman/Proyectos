package com.appgestion.api.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.io.IOException;

/** Coloca logo y título principal en la cabecera del PDF (OpenPDF). */
public final class PdfLogoHelper {

    /** Caja máxima del logo en pt (scaleToFit); algo mayor que antes para mejor legibilidad en PDF. */
    private static final float LOGO_MAX_WIDTH_PT = 145f;
    private static final float LOGO_MAX_HEIGHT_PT = 72f;

    private PdfLogoHelper() {}

    /**
     * Si hay logo: fila con imagen a la izquierda y título a la derecha.
     * Si no: solo el título como párrafo.
     */
    public static void agregarTituloConLogoOpcional(Document document, byte[] logoImagen, String titulo, Font titleFont)
            throws DocumentException, IOException {
        if (logoImagen != null && logoImagen.length > 0) {
            Image img = Image.getInstance(logoImagen);
            img.scaleToFit(LOGO_MAX_WIDTH_PT, LOGO_MAX_HEIGHT_PT);
            PdfPTable row = new PdfPTable(2);
            row.setWidthPercentage(100);
            row.setWidths(new float[]{1.28f, 2.72f});
            PdfPCell cImg = new PdfPCell(img, false);
            cImg.setBorder(Rectangle.NO_BORDER);
            cImg.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cImg.setPadding(0);
            cImg.setPaddingRight(14);
            PdfPCell cTit = new PdfPCell(new Phrase(titulo, titleFont));
            cTit.setBorder(Rectangle.NO_BORDER);
            cTit.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cTit.setPadding(0);
            row.addCell(cImg);
            row.addCell(cTit);
            row.setSpacingAfter(12f);
            document.add(row);
        } else {
            Paragraph title = new Paragraph(titulo, titleFont);
            title.setSpacingAfter(4);
            document.add(title);
        }
    }
}
