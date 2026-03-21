package com.appgestion.api.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Element;

import java.io.IOException;

/** Incrusta imagen de firma en documentos PDF (OpenPDF). */
public final class PdfFirmaHelper {

    private static final float MAX_WIDTH_PT = 140f;

    private PdfFirmaHelper() {}

    public static void agregarFirmaSiExiste(Document document, byte[] firmaImagen, Font smallFont) throws DocumentException, IOException {
        if (firmaImagen == null || firmaImagen.length == 0) {
            return;
        }
        Image img = Image.getInstance(firmaImagen);
        img.scaleToFit(MAX_WIDTH_PT, 80f);
        Paragraph titulo = new Paragraph("Firma:", smallFont);
        titulo.setSpacingBefore(24);
        document.add(titulo);
        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(30);
        wrap.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell cell = new PdfPCell(img, false);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(4);
        wrap.addCell(cell);
        document.add(wrap);
    }
}
