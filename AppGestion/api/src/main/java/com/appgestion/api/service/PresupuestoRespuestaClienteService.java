package com.appgestion.api.service;

import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.PresupuestoEnlace;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.RespuestaClienteRequest;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.PresupuestoEnlaceRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.util.EmailCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
public class PresupuestoRespuestaClienteService {
    private static final Logger log = LoggerFactory.getLogger(PresupuestoRespuestaClienteService.class);
    private static final Duration CHANGE_COOLDOWN = Duration.ofMinutes(5);
    private final PresupuestoEnlaceRepository enlaceRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final EmpresaRepository empresaRepository;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final Clock clock;
    private final String frontendUrl;

    public PresupuestoRespuestaClienteService(
            PresupuestoEnlaceRepository enlaceRepository,
            PresupuestoRepository presupuestoRepository,
            EmpresaRepository empresaRepository,
            NotificacionService notificacionService,
            EmailService emailService,
            Clock clock,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.enlaceRepository = enlaceRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.empresaRepository = empresaRepository;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
        this.clock = clock;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void respond(String token, RespuestaClienteRequest request) {
        PresupuestoEnlace enlace = resolve(token);
        Long presupuestoId = enlace.getPresupuesto().getId();
        Long usuarioId = enlace.getPresupuesto().getUsuario().getId();
        Presupuesto presupuesto = presupuestoRepository.findOwnedForUpdate(presupuestoId, usuarioId)
                .orElseThrow(this::unavailable);
        var empresa = empresaRepository.findByUsuarioId(usuarioId).orElse(null);
        if (empresa != null && !empresa.isPermitirRespuestaCliente()) throw unavailable();
        if (presupuesto.getEnviadoAt() == null || !PresupuestoEstado.isUnresolved(presupuesto.getEstado())) {
            throw unavailable();
        }

        Instant now = clock.instant();
        String previousOption = presupuesto.getRespuestaCliente();
        String previousMessage = presupuesto.getRespuestaClienteMensaje();

        // Cualquier diferencia (opción o mensaje) es una actualización sujeta al cooldown.
        // Si no hay ninguna diferencia, no hace nada.
        boolean sameOption = request.opcion().equals(previousOption);
        boolean sameMessage = Objects.equals(sanitize(request.mensaje()).isBlank() ? null : sanitize(request.mensaje()), previousMessage);
        if (sameOption && sameMessage) return;

        if (previousOption != null && presupuesto.getRespuestaClienteAt() != null
                && presupuesto.getRespuestaClienteAt().plus(CHANGE_COOLDOWN).isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Espera unos minutos para modificar tu aviso");
        }

        String cleanMessage = sanitize(request.mensaje());
        String finalMessage = cleanMessage.isBlank() ? null : cleanMessage;
        // Actualización condicional atómica: incluye los valores anteriores leídos en la condición WHERE.
        // Solo la transacción que leyó esos valores exactos puede actualizar, garantizando que
        // solo una escritura concurrente exita, incluso cuando H2 no reproduce SELECT FOR UPDATE.
        int updated = presupuestoRepository.updateRespuestaClienteIfDistinct(
                presupuestoId, usuarioId, request.opcion(), now, finalMessage, previousOption, previousMessage);
        if (updated == 0) return;
        // Sincroniza la entidad en memoria para lecturas posteriores en la misma transacción.
        presupuesto.setRespuestaCliente(request.opcion());
        presupuesto.setRespuestaClienteAt(now);
        presupuesto.setRespuestaClienteMensaje(finalMessage);

        Usuario owner = presupuesto.getUsuario();
        String clientName = presupuesto.getCliente() != null ? presupuesto.getCliente().getNombre() : "";
        notificacionService.respuestaClientePresupuesto(owner, clientName, presupuestoId, request.opcion());
        try {
            Digest digest = digest(owner.getUiLocale(), clientName, presupuestoId, request.opcion(), finalMessage);
            emailService.enviarRespuestaCliente(owner.getId(), owner.getEmail(), digest.subject(), digest.body(),
                    "respuesta-cliente-" + presupuestoId + "-" + now.toEpochMilli());
        } catch (RuntimeException failure) {
            log.warn("No se pudo encolar respuesta de cliente: presupuestoId={} usuarioId={}",
                    presupuestoId, usuarioId);
        }
    }

    private PresupuestoEnlace resolve(String token) {
        try {
            return enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                            PublicBudgetLinkService.hash(token), clock.instant())
                    .orElseThrow(this::unavailable);
        } catch (IllegalArgumentException ex) {
            throw unavailable();
        }
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Este enlace no está disponible");
    }

    private static String sanitize(String message) {
        if (message == null) return "";
        return message.replaceAll("\\p{Cc}", "").strip();
    }

    private Digest digest(String locale, String clientName, Long budgetId, String option, String message) {
        String language = locale == null ? "es" : locale.split("[-_]")[0].toLowerCase();
        boolean interested = "INTERESA".equals(option);
        String subject = switch (language) {
            case "en" -> "Your customer replied to estimate no. " + budgetId;
            case "fr" -> "Votre client a répondu au devis nº " + budgetId;
            case "ro" -> "Clientul a răspuns la oferta nr. " + budgetId;
            case "uk" -> "Клієнт відповів щодо кошторису № " + budgetId;
            default -> "Tu cliente ha respondido al presupuesto Nº " + budgetId;
        };
        String heading = switch (language) {
            case "en" -> interested ? "is interested in estimate no." : "has questions about estimate no.";
            case "fr" -> interested ? "est intéressé par le devis nº" : "a des questions sur le devis nº";
            case "ro" -> interested ? "este interesat de oferta nr." : "are întrebări despre oferta nr.";
            case "uk" -> interested ? "зацікавлений у кошторисі №" : "має запитання щодо кошторису №";
            default -> interested ? "ha indicado que le interesa el presupuesto Nº" : "tiene dudas sobre el presupuesto Nº";
        };
        String messageLabel = switch (language) {
            case "en" -> "Message";
            case "fr" -> "Message";
            case "ro" -> "Mesaj";
            case "uk" -> "Повідомлення";
            default -> "Mensaje";
        };
        String openApp = switch (language) {
            case "en" -> "Open the budget";
            case "fr" -> "Ouvrir le devis";
            case "ro" -> "Deschide oferta";
            case "uk" -> "Відкрити кошторис";
            default -> "Abrir presupuesto";
        };
        StringBuilder body = new StringBuilder("<p>")
                .append(EmailCopy.htmlEscape(clientName)).append(' ')
                .append(heading).append(' ').append(budgetId).append(".</p>");
        if (message != null && !message.isBlank()) {
            body.append("<p><strong>").append(messageLabel).append(":</strong> ")
                    .append(EmailCopy.htmlEscape(message)).append("</p>");
        }
        body.append("<p><a href=\"").append(EmailCopy.htmlEscape(frontendUrl))
                .append("/presupuestos/").append(budgetId).append("\">").append(openApp).append("</a></p>");
        return new Digest(subject, body.toString());
    }

    private record Digest(String subject, String body) {}
}
