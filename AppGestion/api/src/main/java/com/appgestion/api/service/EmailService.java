package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.dto.support.AdjuntoCorreo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmailService {

    private final EmpresaService empresaService;

    public EmailService(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @SuppressWarnings("null")
    public void enviarPdf(Long usuarioId, String to, String asunto, String cuerpo, byte[] pdf, String nombreArchivo) throws MessagingException {
        EmpresaMail mail = buildEmpresaMail(usuarioId);
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El cliente no tiene email registrado");
        }

        String safeAsunto = Objects.requireNonNullElse(asunto, "");
        String safeCuerpo = Objects.requireNonNullElse(cuerpo, "");
        String safeNombreArchivo = Objects.requireNonNullElse(nombreArchivo, "documento.pdf");
        String toAddress = to.trim();

        MimeMessage message = mail.sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mail.from);
        helper.setTo(toAddress);
        helper.setSubject(safeAsunto);
        helper.setText(safeCuerpo, true);

        if (pdf != null && pdf.length > 0) {
            helper.addAttachment(safeNombreArchivo, () -> new java.io.ByteArrayInputStream(pdf), "application/pdf");
        }

        mail.sender.send(message);
    }

    /**
     * Envía al buzón de soporte usando el SMTP configurado en datos de empresa.
     *
     * @param replyToEmail email del usuario para responder (Reply-To)
     */
    @SuppressWarnings("null")
    public void enviarSoporteConAdjuntos(
            Long usuarioId,
            String to,
            String asunto,
            String cuerpoHtml,
            List<AdjuntoCorreo> adjuntos,
            String replyToEmail
    ) throws MessagingException {
        EmpresaMail mail = buildEmpresaMail(usuarioId);
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Destino de soporte no válido");
        }
        if (!StringUtils.hasText(replyToEmail)) {
            throw new IllegalArgumentException("Email de usuario no válido");
        }

        MimeMessage message = mail.sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mail.from);
        helper.setTo(to.trim());
        helper.setReplyTo(new InternetAddress(replyToEmail.trim()));
        helper.setSubject(Objects.requireNonNullElse(asunto, ""));
        helper.setText(Objects.requireNonNullElse(cuerpoHtml, ""), true);

        if (adjuntos != null) {
            for (AdjuntoCorreo a : adjuntos) {
                if (a == null || a.data() == null || a.data().length == 0) {
                    continue;
                }
                String fn = StringUtils.hasText(a.filename()) ? a.filename() : "adjunto";
                String ct = StringUtils.hasText(a.contentType()) ? a.contentType() : "application/octet-stream";
                helper.addAttachment(fn, () -> new java.io.ByteArrayInputStream(a.data()), ct);
            }
        }

        mail.sender.send(message);
    }

    private EmpresaMail buildEmpresaMail(Long usuarioId) {
        Empresa emp = empresaService.getEmpresaOrNull(usuarioId);
        String fromEmail = emp != null ? emp.getMailUsername() : null;
        String password = emp != null ? emp.getMailPassword() : null;

        if (fromEmail == null || fromEmail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Para enviar al soporte, configura el correo de envío en Configuración → Datos de la empresa → "
                            + "Correo de envío, o bien configura MAIL_USERNAME y MAIL_PASSWORD en el servidor "
                            + "(correo del sistema).");
        }

        String host = "smtp.gmail.com";
        int port = 587;
        if (emp != null) {
            host = (emp.getMailHost() != null && !emp.getMailHost().isBlank()) ? emp.getMailHost() : "smtp.gmail.com";
            port = Optional.ofNullable(emp.getMailPort()).orElse(587);
        }

        String from = Objects.requireNonNull(fromEmail);
        String pwd = Objects.requireNonNull(password);

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(from);
        sender.setPassword(pwd);
        var props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", host);

        return new EmpresaMail(sender, from);
    }

    private record EmpresaMail(JavaMailSenderImpl sender, String from) {}
}
