package com.appgestion.api.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * SEC-33: validación del logo de empresa antes de guardarlo.
 *
 * <p>No basta con limitar los bytes: una imagen pequeña comprimida puede declarar dimensiones enormes y agotar la
 * memoria al decodificarla (p. ej. al generar el PDF). Aquí se lee <b>solo la cabecera</b> (formato y dimensiones)
 * sin decodificar los píxeles, y se aceptan únicamente formatos que el generador de PDF sabe incrustar.
 */
public final class LogoImageValidator {

    public static final int MAX_SIDE_PX = 4000;
    public static final long MAX_PIXELS = 8_000_000L;

    private static final Set<String> FORMATOS_PERMITIDOS = Set.of("png", "jpeg", "gif");

    private LogoImageValidator() {}

    public static final class InvalidLogoException extends RuntimeException {
        public InvalidLogoException(String message) {
            super(message);
        }
    }

    /** Lanza {@link InvalidLogoException} si la imagen no es PNG/JPEG/GIF legible o supera los límites de dimensiones. */
    public static void validar(byte[] data) {
        if (data == null || data.length == 0) {
            throw new InvalidLogoException("El logo está vacío");
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (in == null) {
                throw new InvalidLogoException("El logo debe ser una imagen PNG, JPEG o GIF válida");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new InvalidLogoException("El logo debe ser una imagen PNG, JPEG o GIF válida");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName() == null ? "" : reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!FORMATOS_PERMITIDOS.contains(format)) {
                    throw new InvalidLogoException("El logo debe ser una imagen PNG, JPEG o GIF válida");
                }
                reader.setInput(in, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w <= 0 || h <= 0) {
                    throw new InvalidLogoException("El logo debe ser una imagen PNG, JPEG o GIF válida");
                }
                if (w > MAX_SIDE_PX || h > MAX_SIDE_PX || (long) w * h > MAX_PIXELS) {
                    throw new InvalidLogoException("Las dimensiones del logo no pueden superar " + MAX_SIDE_PX
                            + "x" + MAX_SIDE_PX + " píxeles");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidLogoException ile) {
                throw ile;
            }
            throw new InvalidLogoException("El logo debe ser una imagen PNG, JPEG o GIF válida");
        }
    }
}
