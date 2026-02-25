package com.appgestion.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @SuppressWarnings("null")
    public void enviarPdf(String to, String asunto, String cuerpo, byte[] pdf, String nombreArchivo) throws MessagingException {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("No está configurado el correo de envío (spring.mail.username)");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El cliente no tiene email registrado");
        }
        String safeAsunto = Objects.requireNonNullElse(asunto, "");
        String safeCuerpo = Objects.requireNonNullElse(cuerpo, "");
        String safeNombreArchivo = Objects.requireNonNullElse(nombreArchivo, "documento.pdf");
        String toAddress = to.trim();

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(toAddress);
        helper.setSubject(safeAsunto);
        helper.setText(safeCuerpo, true);

        if (pdf != null && pdf.length > 0) {
            helper.addAttachment(safeNombreArchivo, () -> new java.io.ByteArrayInputStream(pdf), "application/pdf");
        }

        mailSender.send(message);
    }
}
