package com.appgestion.api.controller;

import com.appgestion.api.service.ResendWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
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

    private final ResendWebhookService resendWebhookService;

    public ResendWebhookController(ResendWebhookService resendWebhookService) {
        this.resendWebhookService = resendWebhookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handle(HttpServletRequest request) {
        try {
            String raw = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            resendWebhookService.registrarEventoResend(
                    raw,
                    request.getHeader("svix-id"),
                    request.getHeader("svix-timestamp"),
                    request.getHeader("svix-signature"));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            log.warn("No se pudo leer webhook Resend: {}", e.getMessage());
        }
    }
}
