package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.support.AdjuntoCorreo;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.util.EmailCopy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Envío de solicitudes de soporte al buzón configurado (no expuesto al cliente).
 * Encola el correo; el worker envía según la configuración de la organización (modo system / OAuth / SMTP legacy).
 */
@Service
public class SupportService {

    private static final int MAX_ARCHIVOS = 5;
    private static final long MAX_BYTES_POR_ARCHIVO = 25L * 1024 * 1024;
    private static final long MAX_BYTES_TOTAL_ADJUNTOS = 50L * 1024 * 1024;
    private static final int ASUNTO_MAX = 200;
    private static final int MENSAJE_MIN = 10;
    private static final int MENSAJE_MAX = 8000;

    private static final Set<String> EXTENSIONES_ADJUNTO_SOPORTE = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".mov", ".webm", ".pdf");

    private final EmailService emailService;
    private final EmpresaRepository empresaRepository;
    private final String supportInbox;

    public SupportService(EmailService emailService,
                          EmpresaRepository empresaRepository,
                          @Value("${app.support.inbox-email}") String supportInbox) {
        this.emailService = emailService;
        this.empresaRepository = empresaRepository;
        this.supportInbox = supportInbox != null ? supportInbox.trim() : "";
    }

    public void enviarContactoSoporte(Usuario usuario, String asunto, String mensaje, List<MultipartFile> archivosRaw)
            throws IOException {
        Objects.requireNonNull(usuario, "usuario");
        validarTexto(asunto, mensaje);
        if (supportInbox.isBlank()) {
            throw new IllegalStateException("El buzón de soporte no está configurado en el servidor.");
        }

        List<MultipartFile> archivos = archivosRaw != null ? archivosRaw.stream().filter(f -> f != null && !f.isEmpty()).toList() : List.of();
        if (archivos.size() > MAX_ARCHIVOS) {
            throw new IllegalArgumentException("Máximo " + MAX_ARCHIVOS + " archivos adjuntos.");
        }

        List<AdjuntoCorreo> adjuntos = new ArrayList<>();
        long total = 0;
        for (MultipartFile f : archivos) {
            if (f.getSize() > MAX_BYTES_POR_ARCHIVO) {
                throw new IllegalArgumentException("Cada archivo debe ser de 25 MB como máximo.");
            }
            byte[] contenido = f.getBytes();
            String tipoReal = detectarTipoAdjunto(contenido, f.getOriginalFilename());
            if (tipoReal == null) {
                throw new IllegalArgumentException(
                        "Tipo de archivo no permitido. Usa imágenes, vídeo (p. ej. MP4) o PDF.");
            }
            total += f.getSize();
            if (total > MAX_BYTES_TOTAL_ADJUNTOS) {
                throw new IllegalArgumentException("El tamaño total de los adjuntos supera el límite permitido.");
            }
            String safeName = safeFilename(f.getOriginalFilename());
            // SEC-17: el Content-Type que se reenvia es el detectado por firma, nunca el declarado por el cliente.
            adjuntos.add(new AdjuntoCorreo(safeName, contenido, tipoReal));
        }

        String subject = "[" + EmailCopy.PRODUCT_NAME + " Soporte] " + truncate(asunto.trim(), ASUNTO_MAX);
        String nombreEmpresa = empresaRepository.findByUsuarioId(usuario.getId()).map(e -> e.getNombre()).orElse(null);
        String bodyHtml = EmailCopy.prefijoDestinatarioEmpresa(usuario.getNombre(), nombreEmpresa)
                + buildBodyHtml(usuario, mensaje.trim());

        try {
            emailService.enviarSoporteConAdjuntos(
                    usuario.getId(), supportInbox, subject, bodyHtml, adjuntos, usuario.getEmail());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo enviar el correo. Revisa la configuración de envío o inténtalo más tarde.", e);
        }
    }

    private static void validarTexto(String asunto, String mensaje) {
        if (!StringUtils.hasText(asunto) || asunto.trim().length() < 3) {
            throw new IllegalArgumentException("Indica un asunto (mínimo 3 caracteres).");
        }
        if (asunto.length() > ASUNTO_MAX) {
            throw new IllegalArgumentException("El asunto no puede superar " + ASUNTO_MAX + " caracteres.");
        }
        if (!StringUtils.hasText(mensaje) || mensaje.trim().length() < MENSAJE_MIN) {
            throw new IllegalArgumentException("Describe la incidencia con al menos " + MENSAJE_MIN + " caracteres.");
        }
        if (mensaje.length() > MENSAJE_MAX) {
            throw new IllegalArgumentException("El mensaje no puede superar " + MENSAJE_MAX + " caracteres.");
        }
    }

    private static String buildBodyHtml(Usuario usuario, String mensaje) {
        String esc = escapeHtml(mensaje).replace("\n", "<br>\n");
        return """
                <p><strong>Usuario</strong>: %s &lt;%s&gt; (id: %s)</p>
                <p><strong>Incidencia</strong>:</p>
                <p style="white-space:pre-wrap;">%s</p>
                """.formatted(
                escapeHtml(usuario.getNombre()),
                escapeHtml(usuario.getEmail()),
                usuario.getId(),
                esc
        );
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String safeFilename(String original) {
        if (!StringUtils.hasText(original)) {
            return "adjunto";
        }
        String base = original.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        return base.length() > 180 ? base.substring(0, 180) : base;
    }

    /**
     * SEC-17: valida el adjunto por extension (lista cerrada, sin SVG/HTML) Y por firma de bytes, y comprueba que ambas
     * coinciden. Devuelve el Content-Type real o {@code null} si el archivo no es aceptable.
     */
    static String detectarTipoAdjunto(byte[] b, String originalFilename) {
        if (b == null || b.length < 12) {
            return null;
        }
        String name = originalFilename != null ? originalFilename.toLowerCase(Locale.ROOT) : "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || !EXTENSIONES_ADJUNTO_SOPORTE.contains(name.substring(dot))) {
            return null;
        }
        String ext = name.substring(dot);
        String real = tipoPorFirma(b);
        if (real == null) {
            return null;
        }
        boolean coincide = switch (real) {
            case "image/jpeg" -> ext.equals(".jpg") || ext.equals(".jpeg");
            case "image/png" -> ext.equals(".png");
            case "image/gif" -> ext.equals(".gif");
            case "image/webp" -> ext.equals(".webp");
            case "application/pdf" -> ext.equals(".pdf");
            case "video/mp4", "video/quicktime" -> ext.equals(".mp4") || ext.equals(".mov");
            case "video/webm" -> ext.equals(".webm");
            default -> false;
        };
        return coincide ? real : null;
    }

    private static String tipoPorFirma(byte[] b) {
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
            return "image/gif";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return "application/pdf";
        }
        if (b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            return b[8] == 'q' && b[9] == 't' ? "video/quicktime" : "video/mp4";
        }
        if ((b[0] & 0xFF) == 0x1A && (b[1] & 0xFF) == 0x45 && (b[2] & 0xFF) == 0xDF && (b[3] & 0xFF) == 0xA3) {
            return "video/webm";
        }
        return null;
    }
}