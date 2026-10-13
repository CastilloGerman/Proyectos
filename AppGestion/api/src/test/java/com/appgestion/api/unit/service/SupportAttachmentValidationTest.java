package com.appgestion.api.unit.service;

import com.appgestion.api.service.SupportService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** SEC-17: los adjuntos de soporte se validan por extension + firma de bytes, no por Content-Type declarado. */
class SupportAttachmentValidationTest {

    private static String detectar(byte[] bytes, String nombre) throws Exception {
        Method m = SupportService.class.getDeclaredMethod("detectarTipoAdjunto", byte[].class, String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, bytes, nombre);
    }

    private static byte[] conRelleno(byte[] cabecera) {
        byte[] out = new byte[Math.max(cabecera.length, 32)];
        System.arraycopy(cabecera, 0, out, 0, cabecera.length);
        return out;
    }

    @Test
    void jpegValido_seAcepta() throws Exception {
        byte[] jpg = conRelleno(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
        assertEquals("image/jpeg", detectar(jpg, "foto.JPG"));
    }

    @Test
    void pdfValido_seAcepta() throws Exception {
        byte[] pdf = conRelleno("%PDF-1.7 contenido".getBytes(StandardCharsets.US_ASCII));
        assertEquals("application/pdf", detectar(pdf, "doc.pdf"));
    }

    @Test
    void mp4Valido_seAcepta() throws Exception {
        byte[] mp4 = conRelleno(new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'});
        assertEquals("video/mp4", detectar(mp4, "video.mp4"));
    }

    @Test
    void svgConContentTypeDeImagen_seRechaza() throws Exception {
        byte[] svg = conRelleno("<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8));
        assertNull(detectar(svg, "x.svg"));
        assertNull(detectar(svg, "x.png"));
    }

    @Test
    void htmlRenombradoComoPdf_seRechaza() throws Exception {
        byte[] html = conRelleno("<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
        assertNull(detectar(html, "informe.pdf"));
    }

    @Test
    void extensionNoCoincideConFirma_seRechaza() throws Exception {
        byte[] pdf = conRelleno("%PDF-1.7".getBytes(StandardCharsets.US_ASCII));
        assertNull(detectar(pdf, "foto.png"));
    }

    @Test
    void sinExtensionOContenidoVacio_seRechaza() throws Exception {
        byte[] jpg = conRelleno(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
        assertNull(detectar(jpg, "foto"));
        assertNull(detectar(new byte[0], "foto.jpg"));
        assertNull(detectar(null, "foto.jpg"));
    }
}
