package com.appgestion.api.service.email;

import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.dto.email.EmailJobPayload;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Envío vía Microsoft Graph {@code POST /me/sendMail}.
 */
@Component
public class MicrosoftGraphEmailSender {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MicrosoftGraphEmailSender(ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl("https://graph.microsoft.com").build();
    }

    public void send(String accessToken, Empresa emp, EmailJobPayload payload) {
        String from = emp.getMailUsername() != null && !emp.getMailUsername().isBlank()
                ? emp.getMailUsername().trim()
                : emp.getEmail() != null ? emp.getEmail().trim() : "";
        if (!StringUtils.hasText(from)) {
            throw new IllegalStateException("Outlook: indica email de empresa.");
        }
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode message = root.putObject("message");
        message.put("subject", payload.subject() != null ? payload.subject() : "");
        ObjectNode body = message.putObject("body");
        body.put("contentType", "HTML");
        body.put("content", payload.htmlBody() != null ? payload.htmlBody() : "");
        ArrayNode to = message.putArray("toRecipients");
        ObjectNode addr = to.addObject().putObject("emailAddress");
        addr.put("address", payload.to().trim());
        if (StringUtils.hasText(payload.replyTo())) {
            ArrayNode reply = message.putArray("replyTo");
            ObjectNode r = reply.addObject().putObject("emailAddress");
            r.put("address", payload.replyTo().trim());
        }
        ArrayNode attachments = message.putArray("attachments");
        if (EmailJobPayload.KIND_HTML_PDF.equals(payload.kind()) || EmailJobPayload.KIND_HTML_CLIENT.equals(payload.kind())) {
            if (StringUtils.hasText(payload.pdfBase64())) {
                ObjectNode att = attachments.addObject();
                att.put("@odata.type", "#microsoft.graph.fileAttachment");
                att.put("name", StringUtils.hasText(payload.pdfFilename()) ? payload.pdfFilename() : "documento.pdf");
                att.put("contentType", "application/pdf");
                att.put("contentBytes", payload.pdfBase64());
            }
        } else if (EmailJobPayload.KIND_SUPPORT.equals(payload.kind()) && payload.attachments() != null) {
            for (EmailJobPayload.SupportAttachmentPayload a : payload.attachments()) {
                if (a == null || a.contentBase64() == null) {
                    continue;
                }
                ObjectNode att = attachments.addObject();
                att.put("@odata.type", "#microsoft.graph.fileAttachment");
                att.put("name", StringUtils.hasText(a.filename()) ? a.filename() : "adjunto");
                att.put("contentType", StringUtils.hasText(a.contentType()) ? a.contentType() : "application/octet-stream");
                att.put("contentBytes", a.contentBase64());
            }
        }
        root.put("saveToSentItems", true);

        restClient.post()
                .uri("/v1.0/me/sendMail")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(root.toString())
                .retrieve()
                .toBodilessEntity();
    }
}
