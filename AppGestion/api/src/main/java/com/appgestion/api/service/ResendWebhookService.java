package com.appgestion.api.service;

import com.appgestion.api.config.AppEmailProperties;
import com.appgestion.api.domain.entity.EmailWebhookEvent;
import com.appgestion.api.repository.EmailWebhookEventRepository;
import com.appgestion.api.util.ResendSvixSignatureVerifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResendWebhookService {

    private static final Logger log = LoggerFactory.getLogger(ResendWebhookService.class);

    static final int MAX_PAYLOAD_CHARS = 512_000;

    private final EmailWebhookEventRepository emailWebhookEventRepository;
    private final ObjectMapper objectMapper;
    private final AppEmailProperties appEmailProperties;

    public ResendWebhookService(
            EmailWebhookEventRepository emailWebhookEventRepository,
            ObjectMapper objectMapper,
            AppEmailProperties appEmailProperties) {
        this.emailWebhookEventRepository = emailWebhookEventRepository;
        this.objectMapper = objectMapper;
        this.appEmailProperties = appEmailProperties;
    }

    @Transactional
    public void registrarEventoResend(
            String rawPayload,
            String svixId,
            String svixTimestamp,
            String svixSignature
    ) {
        if (rawPayload == null) {
            rawPayload = "";
        }
        if (rawPayload.length() > MAX_PAYLOAD_CHARS) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Payload demasiado grande");
        }

        String webhookSecret = appEmailProperties.getResend().getWebhookSecret();
        if (StringUtils.hasText(webhookSecret)
                && !ResendSvixSignatureVerifier.verify(rawPayload, svixId, svixTimestamp, svixSignature, webhookSecret)) {
            log.warn("Webhook Resend rechazado: firma inválida o cabeceras ausentes");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firma de webhook inválida");
        }

        EmailWebhookEvent ev = new EmailWebhookEvent();
        ev.setProvider("resend");
        ev.setPayloadJson(rawPayload);
        try {
            JsonNode n = objectMapper.readTree(rawPayload);
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

        try {
            emailWebhookEventRepository.save(ev);
        } catch (DataAccessException e) {
            log.warn("No se pudo persistir webhook Resend: {}", e.getMessage());
            return;
        }

        if (ev.getEventType() != null
                && (ev.getEventType().contains("bounce") || ev.getEventType().contains("complaint"))) {
            log.warn("email_deliverability event={} external_id={}", ev.getEventType(), ev.getExternalId());
        }
    }
}
