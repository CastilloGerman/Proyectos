package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.EmailWebhookEvent;
import com.appgestion.api.repository.EmailWebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * Webhook de Resend (eventos bounce, complaint, etc.). Persiste payload para análisis y alertas.
 */
@RestController
@RequestMapping("/webhook/resend")
public class ResendWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ResendWebhookController.class);

    private final EmailWebhookEventRepository emailWebhookEventRepository;
    private final ObjectMapper objectMapper;

    public ResendWebhookController(
            EmailWebhookEventRepository emailWebhookEventRepository,
            ObjectMapper objectMapper) {
        this.emailWebhookEventRepository = emailWebhookEventRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handle(HttpServletRequest request) {
        try {
            String raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            EmailWebhookEvent ev = new EmailWebhookEvent();
            ev.setProvider("resend");
            ev.setPayloadJson(raw);
            try {
                JsonNode n = objectMapper.readTree(raw);
                if (n.hasNonNull("type")) {
                    ev.setEventType(n.get("type").asText());
                }
                if (n.hasNonNull("data")) {
                    JsonNode data = n.get("data");
                    if (data.hasNonNull("email_id")) {
                        ev.setExternalId(data.get("email_id").asText());
                    }
                }
            } catch (Exception parse) {
                log.debug("Webhook Resend: payload no JSON estándar: {}", parse.getMessage());
            }
            emailWebhookEventRepository.save(ev);
            if (ev.getEventType() != null && (ev.getEventType().contains("bounce") || ev.getEventType().contains("complaint"))) {
                log.warn("email_deliverability event={} external_id={}", ev.getEventType(), ev.getExternalId());
            }
        } catch (Exception e) {
            log.warn("No se pudo persistir webhook Resend: {}", e.getMessage());
        }
    }
}
