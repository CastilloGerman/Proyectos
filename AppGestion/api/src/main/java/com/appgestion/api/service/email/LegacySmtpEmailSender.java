package com.appgestion.api.service.email;

import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.dto.email.EmailJobPayload;
import jakarta.mail.internet.InternetAddress;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * Envío por SMTP legacy (credenciales en {@link Empresa}).
 */
@Component
public class LegacySmtpEmailSender {

    public void send(Empresa emp, EmailJobPayload payload) throws Exception {
        JavaMailSenderImpl sender = buildSender(emp);
        String from = Objects.requireNonNull(emp.getMailUsername()).trim();
        var message = sender.createMimeMessage();
        boolean multipart = EmailJobPayload.KIND_SUPPORT.equals(payload.kind())
                || (payload.pdfBase64() != null && !payload.pdfBase64().isBlank());
        var helper = new MimeMessageHelper(message, multipart, "UTF-8");
        helper.setFrom(from);
        helper.setTo(Objects.requireNonNull(payload.to()).trim());
        helper.setSubject(Objects.requireNonNullElse(payload.subject(), ""));
        helper.setText(Objects.requireNonNullElse(payload.htmlBody(), ""), true);
        if (StringUtils.hasText(payload.replyTo())) {
            helper.setReplyTo(new InternetAddress(payload.replyTo().trim()));
        }

        if (EmailJobPayload.KIND_HTML_PDF.equals(payload.kind()) || EmailJobPayload.KIND_HTML_CLIENT.equals(payload.kind())) {
            if (StringUtils.hasText(payload.pdfBase64())) {
                byte[] pdf = Base64.getDecoder().decode(payload.pdfBase64());
                String fn = StringUtils.hasText(payload.pdfFilename()) ? payload.pdfFilename().trim() : "documento.pdf";
                helper.addAttachment(fn, () -> new ByteArrayInputStream(pdf), "application/pdf");
            }
        } else if (EmailJobPayload.KIND_SUPPORT.equals(payload.kind()) && payload.attachments() != null) {
            for (EmailJobPayload.SupportAttachmentPayload a : payload.attachments()) {
                if (a == null || a.contentBase64() == null) {
                    continue;
                }
                byte[] data = Base64.getDecoder().decode(a.contentBase64());
                String fn = StringUtils.hasText(a.filename()) ? a.filename() : "adjunto";
                String ct = StringUtils.hasText(a.contentType()) ? a.contentType() : "application/octet-stream";
                helper.addAttachment(fn, () -> new ByteArrayInputStream(data), ct);
            }
        }

        sender.send(message);
    }

    private static JavaMailSenderImpl buildSender(Empresa emp) {
        String fromEmail = emp.getMailUsername();
        String password = emp.getMailPassword();
        if (fromEmail == null || fromEmail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("SMTP legacy: faltan credenciales en datos de empresa.");
        }
        String host = (emp.getMailHost() != null && !emp.getMailHost().isBlank()) ? emp.getMailHost() : "smtp.gmail.com";
        int port = Optional.ofNullable(emp.getMailPort()).orElse(587);
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(fromEmail.trim());
        sender.setPassword(password);
        var props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", host);
        return sender;
    }
}
