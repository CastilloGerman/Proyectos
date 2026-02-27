package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Empresa;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.Objects;
import java.util.Optional;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final EmpresaService empresaService;

    public EmailService(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @SuppressWarnings("null")
    public void enviarPdf(Long usuarioId, String to, String asunto, String cuerpo, byte[] pdf, String nombreArchivo) throws MessagingException {
        Empresa emp = empresaService.getEmpresaOrNull(usuarioId);
        String fromEmail = emp != null ? emp.getMailUsername() : null;
        String password = emp != null ? emp.getMailPassword() : null;

        if (fromEmail == null || fromEmail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                "Configure el correo de envío en Configuración → Correo de envío. Use su email y contraseña de aplicación (Gmail: si tiene 2FA, genere una en Google Account).");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El cliente no tiene email registrado");
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

        String safeAsunto = Objects.requireNonNullElse(asunto, "");
        String safeCuerpo = Objects.requireNonNullElse(cuerpo, "");
        String safeNombreArchivo = Objects.requireNonNullElse(nombreArchivo, "documento.pdf");
        String toAddress = to.trim();

        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(toAddress);
        helper.setSubject(safeAsunto);
        helper.setText(safeCuerpo, true);

        if (pdf != null && pdf.length > 0) {
            helper.addAttachment(safeNombreArchivo, () -> new java.io.ByteArrayInputStream(pdf), "application/pdf");
        }

        sender.send(message);
    }
}
