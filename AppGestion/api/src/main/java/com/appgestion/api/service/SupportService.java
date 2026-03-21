package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.support.AdjuntoCorreo;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
 * Usa {@code spring.mail.*} si hay usuario/contraseña; si no, el SMTP de la empresa del usuario.
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

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final EmailService emailService;
    private final String supportInbox;

    public SupportService(
            JavaMailSender javaMailSender,
            MailProperties mailProperties,
            EmailService emailService,
            @Value("${app.support.inbox-email}") String supportInbox
    ) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
        this.emailService = emailService;
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
            if (!isAllowedAttachment(f)) {
                throw new IllegalArgumentException(
                        "Tipo de archivo no permitido. Usa imágenes, vídeo (p. ej. MP4) o PDF.");
            }
            total += f.getSize();
            if (total > MAX_BYTES_TOTAL_ADJUNTOS) {
                throw new IllegalArgumentException("El tamaño total de los adjuntos supera el límite permitido.");
            }
            String safeName = safeFilename(f.getOriginalFilename());
            String ct = StringUtils.hasText(f.getContentType()) ? f.getContentType() : "application/octet-stream";
            adjuntos.add(new AdjuntoCorreo(safeName, f.getBytes(), ct));
        }

        String subject = "[AppGestion Soporte] " + truncate(asunto.trim(), ASUNTO_MAX);
        String bodyHtml = buildBodyHtml(usuario, mensaje.trim());

        try {
            if (canUseSystemMail()) {
                enviarConSpringMail(usuario, subject, bodyHtml, adjuntos);
            } else {
                emailService.enviarSoporteConAdjuntos(
                        usuario.getId(), supportInbox, subject, bodyHtml, adjuntos, usuario.getEmail());
            }
        } catch (MessagingException e) {
            throw new IllegalStateException(
                    "No se pudo enviar el correo. Revisa la configuración SMTP o inténtalo más tarde.", e);
        }
    }

    private boolean canUseSystemMail() {
        String u = mailProperties.getUsername();
        String p = mailProperties.getPassword();
        return StringUtils.hasText(u) && StringUtils.hasText(p);
    }

    private void enviarConSpringMail(Usuario usuario, String subject, String bodyHtml, List<AdjuntoCorreo> adjuntos)
            throws MessagingException {
        var message = javaMailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, "UTF-8");
        String from = Objects.requireNonNull(
                Objects.requireNonNull(mailProperties.getUsername(), "mail username").trim(), "from");
        String inbox = Objects.requireNonNull(supportInbox, "support inbox");
        String replyTo = Objects.requireNonNull(
                Objects.requireNonNull(usuario.getEmail(), "usuario email").trim(), "replyTo");
        String subj = Objects.requireNonNull(subject, "subject");
        String body = Objects.requireNonNull(bodyHtml, "body");
        helper.setFrom(from);
        helper.setTo(inbox);
        helper.setReplyTo(replyTo);
        helper.setSubject(subj);
        helper.setText(body, true);
        for (AdjuntoCorreo a : adjuntos) {
            String fn = Objects.requireNonNull(Objects.requireNonNullElse(a.filename(), "adjunto"), "filename");
            String ct = Objects.requireNonNull(
                    Objects.requireNonNullElse(a.contentType(), "application/octet-stream"), "contentType");
            helper.addAttachment(
                    fn,
                    () -> new java.io.ByteArrayInputStream(Objects.requireNonNull(a.data(), "adjunto data")),
                    ct
            );
        }
        javaMailSender.send(message);
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

    private static boolean isAllowedAttachment(MultipartFile f) {
        String ct = f.getContentType();
        if (ct != null && StringUtils.hasText(ct)) {
            String c = ct.toLowerCase(Locale.ROOT);
            if (c.startsWith("image/")) {
                return true;
            }
            if (c.startsWith("video/")) {
                return true;
            }
            if ("application/pdf".equals(c)) {
                return true;
            }
        }
        String orig = f.getOriginalFilename();
        String filenameLower = orig != null ? orig.toLowerCase(Locale.ROOT) : "";
        return extensionAllowedForSupport(filenameLower);
    }

    private static boolean extensionAllowedForSupport(String filenameLower) {
        int dot = filenameLower.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return EXTENSIONES_ADJUNTO_SOPORTE.contains(filenameLower.substring(dot));
    }
}
