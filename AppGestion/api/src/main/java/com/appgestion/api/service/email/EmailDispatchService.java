package com.appgestion.api.service.email;

import com.appgestion.api.config.AppEmailProperties;
import com.appgestion.api.domain.entity.EmailJob;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.enums.EmailProviderMode;
import com.appgestion.api.domain.enums.OAuthOnFailureMode;
import com.appgestion.api.dto.email.EmailJobPayload;
import com.appgestion.api.repository.EmpresaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Envía un trabajo de cola según la estrategia de la organización (system / OAuth / SMTP legacy).
 */
@Service
public class EmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchService.class);

    private final ObjectMapper objectMapper;
    private final EmpresaRepository empresaRepository;
    private final AppEmailProperties appEmailProperties;
    private final ResendEmailClient resendEmailClient;
    private final LegacySmtpEmailSender legacySmtpEmailSender;
    private final GmailApiEmailSender gmailApiEmailSender;
    private final MicrosoftGraphEmailSender microsoftGraphEmailSender;
    private final OAuthTokenRefreshService oauthTokenRefreshService;

    public EmailDispatchService(
            ObjectMapper objectMapper,
            EmpresaRepository empresaRepository,
            AppEmailProperties appEmailProperties,
            ResendEmailClient resendEmailClient,
            LegacySmtpEmailSender legacySmtpEmailSender,
            GmailApiEmailSender gmailApiEmailSender,
            MicrosoftGraphEmailSender microsoftGraphEmailSender,
            OAuthTokenRefreshService oauthTokenRefreshService) {
        this.objectMapper = objectMapper;
        this.empresaRepository = empresaRepository;
        this.appEmailProperties = appEmailProperties;
        this.resendEmailClient = resendEmailClient;
        this.legacySmtpEmailSender = legacySmtpEmailSender;
        this.gmailApiEmailSender = gmailApiEmailSender;
        this.microsoftGraphEmailSender = microsoftGraphEmailSender;
        this.oauthTokenRefreshService = oauthTokenRefreshService;
    }

    public void dispatch(EmailJob job) throws Exception {
        EmailJobPayload payload = objectMapper.readValue(job.getPayloadJson(), EmailJobPayload.class);
        Empresa emp = empresaRepository.findByUsuarioId(job.getUsuario().getId())
                .orElseThrow(() -> new IllegalStateException("Empresa no encontrada para el usuario."));
        EmailProviderMode mode = emp.getEmailProvider() != null ? emp.getEmailProvider() : EmailProviderMode.system;

        try {
            sendWithMode(mode, emp, payload);
        } catch (Exception primary) {
            if ((mode == EmailProviderMode.gmail || mode == EmailProviderMode.outlook)
                    && emp.getOauthOnFailure() == OAuthOnFailureMode.system) {
                log.warn("email_dispatch_failed provider={} org_id={} reason={} — fallback system",
                        mode, emp.getId(), primary.getMessage());
                sendSystem(emp, payload);
                return;
            }
            log.warn("email_dispatch_failed provider={} org_id={} reason={}", mode, emp.getId(), primary.getMessage());
            throw primary;
        }
    }

    private void sendWithMode(EmailProviderMode mode, Empresa emp, EmailJobPayload payload) throws Exception {
        switch (mode) {
            case system -> sendSystem(emp, payload);
            case smtp_legacy -> legacySmtpEmailSender.send(emp, payload);
            case gmail -> {
                String access = oauthTokenRefreshService.getGoogleAccessToken(emp);
                gmailApiEmailSender.send(access, emp, payload);
            }
            case outlook -> {
                String access = oauthTokenRefreshService.getMicrosoftAccessToken(emp);
                microsoftGraphEmailSender.send(access, emp, payload);
            }
            default -> sendSystem(emp, payload);
        }
    }

    private void sendSystem(Empresa emp, EmailJobPayload payload) {
        String from = StringUtils.hasText(emp.getSystemFromOverride())
                ? emp.getSystemFromOverride().trim()
                : appEmailProperties.getSystemFrom();
        List<ResendEmailClient.Attachment> attachments = new ArrayList<>();
        if (EmailJobPayload.KIND_HTML_PDF.equals(payload.kind()) || EmailJobPayload.KIND_HTML_CLIENT.equals(payload.kind())) {
            if (StringUtils.hasText(payload.pdfBase64())) {
                attachments.add(new ResendEmailClient.Attachment(
                        StringUtils.hasText(payload.pdfFilename()) ? payload.pdfFilename() : "documento.pdf",
                        payload.pdfBase64(),
                        "application/pdf"));
            }
        } else if (EmailJobPayload.KIND_SUPPORT.equals(payload.kind()) && payload.attachments() != null) {
            for (EmailJobPayload.SupportAttachmentPayload a : payload.attachments()) {
                if (a == null || a.contentBase64() == null) {
                    continue;
                }
                attachments.add(new ResendEmailClient.Attachment(
                        StringUtils.hasText(a.filename()) ? a.filename() : "adjunto",
                        a.contentBase64(),
                        StringUtils.hasText(a.contentType()) ? a.contentType() : "application/octet-stream"));
            }
        }
        resendEmailClient.send(
                from,
                payload.to(),
                payload.subject() != null ? payload.subject() : "",
                payload.htmlBody() != null ? payload.htmlBody() : "",
                payload.replyTo(),
                attachments.isEmpty() ? List.of() : attachments);
    }
}
