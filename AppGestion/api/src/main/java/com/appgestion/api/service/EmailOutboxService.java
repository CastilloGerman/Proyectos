package com.appgestion.api.service;

import com.appgestion.api.domain.entity.EmailJob;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.email.EmailJobPayload;
import com.appgestion.api.dto.support.AdjuntoCorreo;
import com.appgestion.api.repository.EmailJobRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Encola correos en {@code email_jobs}; el worker envía de forma asíncrona.
 */
@Service
public class EmailOutboxService {

    private final EmailJobRepository emailJobRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public EmailOutboxService(
            EmailJobRepository emailJobRepository,
            UsuarioRepository usuarioRepository,
            ObjectMapper objectMapper) {
        this.emailJobRepository = emailJobRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueueHtmlPdf(
            Long usuarioId,
            String to,
            String subject,
            String htmlBody,
            byte[] pdf,
            String pdfFilename,
            String idempotencyKey
    ) {
        String pdfB64 = (pdf != null && pdf.length > 0) ? Base64.getEncoder().encodeToString(pdf) : null;
        EmailJobPayload payload = new EmailJobPayload(
                EmailJobPayload.KIND_HTML_PDF,
                to,
                subject,
                htmlBody,
                null,
                pdfB64,
                pdfFilename,
                null);
        saveJob(usuarioId, idempotencyKey, payload);
    }

    @Transactional
    public void enqueueHtmlCliente(
            Long usuarioId,
            String to,
            String replyToEmail,
            String subject,
            String htmlBody,
            byte[] pdf,
            String pdfFilename,
            String idempotencyKey
    ) {
        String pdfB64 = (pdf != null && pdf.length > 0) ? Base64.getEncoder().encodeToString(pdf) : null;
        EmailJobPayload payload = new EmailJobPayload(
                EmailJobPayload.KIND_HTML_CLIENT,
                to,
                subject,
                htmlBody,
                replyToEmail,
                pdfB64,
                pdfFilename,
                null);
        saveJob(usuarioId, idempotencyKey, payload);
    }

    @Transactional
    public void enqueueSoporte(
            Long usuarioId,
            String to,
            String subject,
            String htmlBody,
            List<AdjuntoCorreo> adjuntos,
            String replyToEmail,
            String idempotencyKey
    ) {
        List<EmailJobPayload.SupportAttachmentPayload> list = null;
        if (adjuntos != null && !adjuntos.isEmpty()) {
            list = adjuntos.stream()
                    .filter(a -> a != null && a.data() != null && a.data().length > 0)
                    .map(a -> new EmailJobPayload.SupportAttachmentPayload(
                            Objects.requireNonNullElse(a.filename(), "adjunto"),
                            Base64.getEncoder().encodeToString(a.data()),
                            Objects.requireNonNullElse(a.contentType(), "application/octet-stream")))
                    .toList();
        }
        EmailJobPayload payload = new EmailJobPayload(
                EmailJobPayload.KIND_SUPPORT,
                to,
                subject,
                htmlBody,
                replyToEmail,
                null,
                null,
                list);
        saveJob(usuarioId, idempotencyKey, payload);
    }

    private void saveJob(Long usuarioId, String idempotencyKey, EmailJobPayload payload) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        try {
            EmailJob job = new EmailJob();
            job.setUsuario(usuario);
            job.setIdempotencyKey(idempotencyKey);
            job.setPayloadJson(objectMapper.writeValueAsString(payload));
            emailJobRepository.save(job);
        } catch (DataIntegrityViolationException dup) {
            // idempotencia: mismo trabajo ya encolado
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo encolar el correo", e);
        }
    }
}
