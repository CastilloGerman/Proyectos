package com.appgestion.api.service.email;

import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.dto.email.EmailJobPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

/**
 * Envío vía Gmail API ({@code users.messages.send}) con MIME raw en Base64URL.
 */
@Component
public class GmailApiEmailSender {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GmailApiEmailSender(ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl("https://gmail.googleapis.com").build();
    }

    public void send(String accessToken, Empresa emp, EmailJobPayload payload) throws Exception {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        String from = emp.getMailUsername() != null && !emp.getMailUsername().isBlank()
                ? emp.getMailUsername().trim()
                : emp.getEmail() != null ? emp.getEmail().trim() : "";
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("Gmail: indica email de empresa o usuario de correo.");
        }
        boolean multipart = EmailJobPayload.KIND_SUPPORT.equals(payload.kind())
                || (payload.pdfBase64() != null && !payload.pdfBase64().isBlank());
        var helper = new MimeMessageHelper(message, multipart, "UTF-8");
        helper.setFrom(new InternetAddress(from));
        helper.setTo(payload.to().trim());
        helper.setSubject(payload.subject() != null ? payload.subject() : "");
        helper.setText(payload.htmlBody() != null ? payload.htmlBody() : "", true);
        if (StringUtils.hasText(payload.replyTo())) {
            helper.setReplyTo(new InternetAddress(payload.replyTo().trim()));
        }
        if (EmailJobPayload.KIND_HTML_PDF.equals(payload.kind()) || EmailJobPayload.KIND_HTML_CLIENT.equals(payload.kind())) {
            if (StringUtils.hasText(payload.pdfBase64())) {
                byte[] pdf = Base64.getDecoder().decode(payload.pdfBase64());
                String fn = StringUtils.hasText(payload.pdfFilename()) ? payload.pdfFilename().trim() : "documento.pdf";
                helper.addAttachment(fn, () -> new java.io.ByteArrayInputStream(pdf), "application/pdf");
            }
        } else if (EmailJobPayload.KIND_SUPPORT.equals(payload.kind()) && payload.attachments() != null) {
            for (EmailJobPayload.SupportAttachmentPayload a : payload.attachments()) {
                if (a == null || a.contentBase64() == null) {
                    continue;
                }
                byte[] data = Base64.getDecoder().decode(a.contentBase64());
                String fn = StringUtils.hasText(a.filename()) ? a.filename() : "adjunto";
                String ct = StringUtils.hasText(a.contentType()) ? a.contentType() : "application/octet-stream";
                helper.addAttachment(fn, () -> new java.io.ByteArrayInputStream(data), ct);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());

        ObjectMapper om = objectMapper;
        String bodyJson = om.createObjectNode().put("raw", raw).toString();

        restClient.post()
                .uri("/gmail/v1/users/me/messages/send")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyJson)
                .retrieve()
                .body(JsonNode.class);
    }
}
