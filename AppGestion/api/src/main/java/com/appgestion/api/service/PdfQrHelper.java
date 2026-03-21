package com.appgestion.api.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** QR en PDF (enlace de pago o referencia del documento). */
public final class PdfQrHelper {

    private static final int QR_PX = 140;

    private PdfQrHelper() {}

    public static void agregarQr(Document document, String contenido, Font smallFont) throws DocumentException, IOException {
        if (contenido == null || contenido.isBlank()) {
            return;
        }
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(contenido, BarcodeFormat.QR_CODE, QR_PX, QR_PX);
            BufferedImage bi = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "PNG", baos);
            Image img = Image.getInstance(baos.toByteArray());
            img.scaleToFit(72f, 72f);
            Paragraph titulo = new Paragraph("Pago / referencia (escanea):", smallFont);
            titulo.setSpacingBefore(16);
            document.add(titulo);
            PdfPTable wrap = new PdfPTable(1);
            wrap.setWidthPercentage(22);
            wrap.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfPCell cell = new PdfPCell(img, false);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingTop(4);
            wrap.addCell(cell);
            document.add(wrap);
        } catch (WriterException e) {
            throw new IOException("Error generando QR", e);
        }
    }
}
