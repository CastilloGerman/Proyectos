package com.appgestion.api.service;

import com.appgestion.api.dto.support.AdjuntoCorreo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * API de envío de correo: solo encola trabajos; el worker envía de forma asíncrona.
 */
@Service
public class EmailService {

    private final EmailOutboxService emailOutboxService;

    public EmailService(EmailOutboxService emailOutboxService) {
        this.emailOutboxService = emailOutboxService;
    }

    public void enviarPdf(Long usuarioId, String to, String asunto, String cuerpo, byte[] pdf, String nombreArchivo) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El cliente no tiene email registrado");
        }
        String key = "pdf-" + usuarioId + "-" + UUID.randomUUID();
        emailOutboxService.enqueueHtmlPdf(usuarioId, to, asunto, cuerpo, pdf, nombreArchivo, key);
    }

    public void enviarHtmlACliente(
            Long usuarioId,
            String to,
            String replyToEmail,
            String asunto,
            String cuerpoHtml
    ) {
        enviarHtmlACliente(usuarioId, to, replyToEmail, asunto, cuerpoHtml, null, null);
    }

    public void enviarHtmlACliente(
            Long usuarioId,
            String to,
            String replyToEmail,
            String asunto,
            String cuerpoHtml,
            byte[] pdfAdjunto,
            String nombreArchivoAdjunto
    ) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("El destinatario no tiene email");
        }
        String key = "html-client-" + usuarioId + "-" + UUID.randomUUID();
        emailOutboxService.enqueueHtmlCliente(
                usuarioId, to, replyToEmail, asunto, cuerpoHtml, pdfAdjunto, nombreArchivoAdjunto, key);
    }

    public void enviarSoporteConAdjuntos(
            Long usuarioId,
            String to,
            String asunto,
            String cuerpoHtml,
            List<AdjuntoCorreo> adjuntos,
            String replyToEmail
    ) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Destino de soporte no válido");
        }
        if (replyToEmail == null || replyToEmail.isBlank()) {
            throw new IllegalArgumentException("Email de usuario no válido");
        }
        String key = "support-" + usuarioId + "-" + UUID.randomUUID();
        emailOutboxService.enqueueSoporte(usuarioId, to, asunto, cuerpoHtml, adjuntos, replyToEmail, key);
    }
}
