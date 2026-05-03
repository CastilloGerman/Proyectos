package com.appgestion.api.service.email;

import com.appgestion.api.config.AppEmailProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP para envío transaccional vía Resend (modo {@code system}).
 */
@Component
public class ResendEmailClient {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailClient.class);

    private final AppEmailProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ResendEmailClient(AppEmailProperties props, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.baseUrl("https://api.resend.com").build();
    }

    public void send(
            String from,
            String to,
            String subject,
            String htmlBody,
            String replyTo,
            List<Attachment> attachments
    ) {
        String apiKey = props.getResend().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("RESEND_API_KEY / app.email.resend.api-key no configurado.");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("from", from);
        ArrayNode toArr = body.putArray("to");
        toArr.add(to);
        body.put("subject", subject);
        body.put("html", htmlBody);
        if (StringUtils.hasText(replyTo)) {
            body.put("reply_to", replyTo.trim());
        }
        if (attachments != null && !attachments.isEmpty()) {
            ArrayNode arr = body.putArray("attachments");
            for (Attachment a : attachments) {
                ObjectNode o = arr.addObject();
                o.put("filename", a.filename());
                o.put("content", a.contentBase64());
                if (StringUtils.hasText(a.contentType())) {
                    o.put("content_type", a.contentType());
                }
            }
        }

        String json = body.toString();
        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            String snippet = "";
            try {
                String bodyStr = e.getResponseBodyAsString(StandardCharsets.UTF_8);
                if (StringUtils.hasText(bodyStr)) {
                    snippet = bodyStr.length() > 512 ? bodyStr.substring(0, 512) + "…" : bodyStr;
                }
            } catch (Exception ignored) {
                snippet = "(sin cuerpo)";
            }
            log.warn(
                    "email_dispatch_failed provider=resend http_status={} detail={}",
                    e.getStatusCode().value(),
                    snippet);
            throw new IllegalStateException(
                    "Resend rechazó el envío (%d): %s".formatted(e.getStatusCode().value(), snippet),
                    e);
        } catch (Exception e) {
            log.warn("email_dispatch_failed provider=resend reason={}", e.getMessage());
            throw new IllegalStateException("Resend rechazó el envío: " + e.getMessage(), e);
        }
    }

    public record Attachment(String filename, String contentBase64, String contentType) {}
}
