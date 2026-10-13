package com.appgestion.api.unit.util;

import com.appgestion.api.util.LogoImageValidator;
import com.appgestion.api.util.LogoImageValidator.InvalidLogoException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SEC-33: el logo debe ser una imagen real PNG/JPEG/GIF con dimensiones acotadas. */
class LogoImageValidatorTest {

    private static byte[] imagen(String formato, int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, formato, out);
        return out.toByteArray();
    }

    @Test
    void aceptaPngJpegGifValidos() throws Exception {
        assertThatCode(() -> LogoImageValidator.validar(imagen("png", 200, 80))).doesNotThrowAnyException();
        assertThatCode(() -> LogoImageValidator.validar(imagen("jpg", 200, 80))).doesNotThrowAnyException();
        assertThatCode(() -> LogoImageValidator.validar(imagen("gif", 200, 80))).doesNotThrowAnyException();
    }

    @Test
    void rechazaContenidoQueNoEsImagen() {
        assertThatThrownBy(() -> LogoImageValidator.validar("<svg onload=alert(1)/>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidLogoException.class);
        assertThatThrownBy(() -> LogoImageValidator.validar("%PDF-1.7".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidLogoException.class);
        assertThatThrownBy(() -> LogoImageValidator.validar(new byte[0])).isInstanceOf(InvalidLogoException.class);
        assertThatThrownBy(() -> LogoImageValidator.validar(null)).isInstanceOf(InvalidLogoException.class);
    }

    @Test
    void rechazaImagenTruncada() throws Exception {
        byte[] png = imagen("png", 50, 50);
        byte[] cortado = java.util.Arrays.copyOf(png, 20);
        assertThatThrownBy(() -> LogoImageValidator.validar(cortado)).isInstanceOf(InvalidLogoException.class);
    }

    @Test
    void rechazaDimensionesExcesivas() throws Exception {
        // PNG de color plano: pocos bytes pero 5000x10 px / 4001 px de lado excede el máximo por lado.
        assertThatThrownBy(() -> LogoImageValidator.validar(imagen("png", LogoImageValidator.MAX_SIDE_PX + 1, 4)))
                .isInstanceOf(InvalidLogoException.class)
                .hasMessageContaining("dimensiones");
    }

    @Test
    void rechazaDemasiadosPixelesAunqueCadaLadoSeaValido() throws Exception {
        assertThatThrownBy(() -> LogoImageValidator.validar(imagen("png", 3500, 3500)))
                .isInstanceOf(InvalidLogoException.class);
    }
}
