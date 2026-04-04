package com.appgestion.api.controller;

import com.appgestion.api.config.AppEmailProperties;
import com.appgestion.api.domain.entity.EmailWebhookEvent;
import com.appgestion.api.repository.EmailWebhookEventRepository;
import com.appgestion.api.util.ResendSvixSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Webhook de Resend (eventos bounce, complaint, etc.). Persiste payload para análisis y alertas.
 * Si {@code app.email.resend.webhook-secret} está definido, exige cabeceras Svix y firma válida.
 */
@RestController
@RequestMapping("/webhook/resend")
public class ResendWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ResendWebhookController.class);

    private static final int MAX_PAYLOAD_CHARS = 512_000;

    private final EmailWebhookEventRepository emailWebhookEventRepository;
    private final ObjectMapper objectMapper;
    private final AppEmailProperties appEmailProperties;

    public ResendWebhookController(
            EmailWebhookEventRepository emailWebhookEventRepository,
            ObjectMapper objectMapper,
            AppEmailProperties appEmailProperties) {
        this.emailWebhookEventRepository = emailWebhookEventRepository;
        this.objectMapper = objectMapper;
        this.appEmailProperties = appEmailProperties;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handle(HttpServletRequest request) {
        try {
            String raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            if (raw.length() > MAX_PAYLOAD_CHARS) {
                throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Payload demasiado grande");
            }

            String webhookSecret = appEmailProperties.getResend().getWebhookSecret();
            if (StringUtils.hasText(webhookSecret)) {
                String svixId = request.getHeader("svix-id");
                String svixTs = request.getHeader("svix-timestamp");
                String svixSig = request.getHeader("svix-signature");
                if (!ResendSvixSignatureVerifier.verify(raw, svixId, svixTs, svixSig, webhookSecret)) {
                    log.warn("Webhook Resend rechazado: firma inválida o cabeceras ausentes");
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firma de webhook inválida");
                }
            }

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
            } catch (JsonProcessingException parse) {
                log.debug("Webhook Resend: payload no JSON estándar: {}", parse.getMessage());
            }
            emailWebhookEventRepository.save(ev);
            if (ev.getEventType() != null && (ev.getEventType().contains("bounce") || ev.getEventType().contains("complaint"))) {
                log.warn("email_deliverability event={} external_id={}", ev.getEventType(), ev.getExternalId());
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException | DataAccessException e) {
            log.warn("No se pudo persistir webhook Resend: {}", e.getMessage());
        }
    }
}
