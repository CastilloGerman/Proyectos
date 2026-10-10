package com.appgestion.api.service;

import com.appgestion.api.constant.PresupuestoEstado;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.dto.request.SeguimientoPresupuestosPatchRequest;
import com.appgestion.api.dto.response.SeguimientoPresupuestosResponse;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.PresupuestoEnlaceRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.repository.PresupuestoSeguimientoAvisoRepository;
import com.appgestion.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class PresupuestoSeguimientoService {
    private static final Logger log = LoggerFactory.getLogger(PresupuestoSeguimientoService.class);
    private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

    private final EmpresaRepository empresaRepository;
    private final PresupuestoRepository presupuestoRepository;
    private final PresupuestoEnlaceRepository enlaceRepository;
    private final PresupuestoSeguimientoAvisoRepository avisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final Clock clock;
    private final Environment environment;
    private final String frontendUrl;
    private final int localDaysOverride;

    public PresupuestoSeguimientoService(
            EmpresaRepository empresaRepository,
            PresupuestoRepository presupuestoRepository,
            PresupuestoEnlaceRepository enlaceRepository,
            PresupuestoSeguimientoAvisoRepository avisoRepository,
            UsuarioRepository usuarioRepository,
            NotificacionService notificacionService,
            EmailService emailService,
            Clock clock,
            Environment environment,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.presupuesto-seguimiento.local-days-override:-1}") int localDaysOverride) {
        this.empresaRepository = empresaRepository;
        this.presupuestoRepository = presupuestoRepository;
        this.enlaceRepository = enlaceRepository;
        this.avisoRepository = avisoRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
        this.clock = clock;
        this.environment = environment;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.localDaysOverride = localDaysOverride;
    }

    @Transactional(readOnly = true)
    public SeguimientoPresupuestosResponse getSettings(Long usuarioId) {
        return empresaRepository.findByUsuarioId(usuarioId)
                .map(this::toResponse)
                .orElseGet(() -> new SeguimientoPresupuestosResponse(true, 3, 2, false, true));
    }

    @Transactional
    public SeguimientoPresupuestosResponse patchSettings(Long usuarioId, SeguimientoPresupuestosPatchRequest request) {
        if (request == null
                || (request.seguimientoDiasEspera() != null
                    && (request.seguimientoDiasEspera() < 1 || request.seguimientoDiasEspera() > 30))
                || (request.seguimientoMaxAvisos() != null
                    && (request.seguimientoMaxAvisos() < 1 || request.seguimientoMaxAvisos() > 5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preferencias de seguimiento no válidas");
        }
        Empresa empresa = empresaRepository.findByUsuarioId(usuarioId).orElseGet(() -> {
            Usuario owner = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            Empresa created = new Empresa();
            created.setUsuario(owner);
            created.setNombre(owner.getNombre());
            return created;
        });
        if (request.seguimientoActivo() != null) empresa.setSeguimientoActivo(request.seguimientoActivo());
        if (request.seguimientoDiasEspera() != null) empresa.setSeguimientoDiasEspera(request.seguimientoDiasEspera());
        if (request.seguimientoMaxAvisos() != null) empresa.setSeguimientoMaxAvisos(request.seguimientoMaxAvisos());
        if (request.seguimientoEmailResumen() != null) empresa.setSeguimientoEmailResumen(request.seguimientoEmailResumen());
        if (request.permitirRespuestaCliente() != null) {
            empresa.setPermitirRespuestaCliente(request.permitirRespuestaCliente());
        }
        return toResponse(empresaRepository.save(empresa));
    }

    @Transactional
    public void setSilenced(Long presupuestoId, Long usuarioId, boolean silenced) {
        Presupuesto presupuesto = presupuestoRepository.findOwnedForUpdate(presupuestoId, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
        presupuesto.setSeguimientoSilenciado(silenced);
        presupuestoRepository.save(presupuesto);
    }

    @Transactional
    public int processOwner(Long usuarioId, Integer daysOverride) {
        Usuario owner = usuarioRepository.findById(usuarioId).orElse(null);
        Empresa settings = empresaRepository.findByUsuarioId(usuarioId).orElse(null);
        if (!isEligibleOwner(owner) || (settings != null && !settings.isSeguimientoActivo())) return 0;
        int configuredDays = settings != null ? settings.getSeguimientoDiasEspera() : 3;
        int maximumAlerts = settings != null ? settings.getSeguimientoMaxAvisos() : 2;
        boolean emailSummary = settings != null && settings.isSeguimientoEmailResumen();
        int waitDays = effectiveDays(configuredDays, daysOverride);
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        Instant now = clock.instant();
        List<NewAlert> newAlerts = new ArrayList<>();

        Long lastBudgetId = 0L;
        List<Presupuesto> batch;
        do {
            batch = presupuestoRepository
                    .findByUsuarioIdAndEnviadoAtIsNotNullAndSeguimientoSilenciadoFalseAndIdGreaterThanOrderByIdAsc(
                            usuarioId, lastBudgetId, PageRequest.of(0, 100));
            for (Presupuesto candidate : batch) {
                lastBudgetId = candidate.getId();
                Presupuesto presupuesto = presupuestoRepository.findOwnedForUpdate(candidate.getId(), usuarioId).orElse(null);
                if (presupuesto == null || presupuesto.isSeguimientoSilenciado()
                        || presupuesto.getEnviadoAt() == null
                        || !PresupuestoEstado.isUnresolved(presupuesto.getEstado())
                        || presupuesto.getRespuestaCliente() != null) continue;

                Instant latestView = enlaceRepository.latestViewAt(presupuesto.getId());
                String type = latestView == null ? "NO_ABIERTO" : "ABIERTO_SIN_RESPUESTA";
                LocalDate anchorDate = latestView == null
                        ? presupuesto.getEnviadoAt().atZone(ZONE).toLocalDate()
                        : latestView.atZone(ZONE).toLocalDate();
                long elapsedDays = ChronoUnit.DAYS.between(anchorDate, today);
                if (elapsedDays < waitDays) continue;

                long existing = avisoRepository.countByPresupuestoId(presupuesto.getId());
                if (existing >= maximumAlerts) continue;
                var previous = avisoRepository.findTopByPresupuestoIdOrderByCreadoAtDesc(presupuesto.getId());
                if (previous.isPresent()) {
                    LocalDate previousDate = previous.get().getCreadoAt().atZone(ZONE).toLocalDate();
                    long daysSincePrevious = ChronoUnit.DAYS.between(previousDate, today);
                    if (previousDate.equals(today) || ((daysOverride == null || daysOverride > 0)
                            && daysSincePrevious < waitDays)) continue;
                }

                int number = Math.toIntExact(existing + 1);
                if (avisoRepository.insertIfAbsent(presupuesto.getId(), type, number, now) != 1) continue;
                String clientName = presupuesto.getCliente() != null ? presupuesto.getCliente().getNombre() : "";
                notificacionService.presupuestoSeguimiento(owner, clientName, presupuesto.getId(), type, elapsedDays);
                newAlerts.add(new NewAlert(presupuesto.getId(), clientName, type, elapsedDays));
            }
        }
        while (batch.size() == 100);

        if (emailSummary && !newAlerts.isEmpty()) {
            Digest digest = digest(owner.getUiLocale(), newAlerts);
            String key = "presupuesto-seguimiento-" + usuarioId + "-" + today;
            try {
                emailService.enviarResumenSeguimiento(
                        usuarioId, owner.getEmail(), digest.subject(), digest.body(), key);
            } catch (RuntimeException failure) {
                log.warn("No se pudo encolar resumen de seguimiento: usuarioId={} avisos={} errores=1",
                        usuarioId, newAlerts.size());
            }
        }
        return newAlerts.size();
    }

    public Integer localDaysOverride() {
        if (!isLocalProfile() || localDaysOverride < 0 || localDaysOverride > 30) {
            throw new IllegalStateException("La ejecución manual local no tiene un plazo permitido");
        }
        return localDaysOverride;
    }

    private int effectiveDays(int configured, Integer override) {
        if (override == null) return configured;
        if (override < 0 || override > 30
                || (override == 0 && (!isLocalProfile() || localDaysOverride != 0))) {
            throw new IllegalArgumentException("El plazo de seguimiento debe estar entre 1 y 30 días");
        }
        return override;
    }

    private boolean isLocalProfile() {
        return environment.acceptsProfiles(Profiles.of("local"));
    }

    private boolean isEligibleOwner(Usuario owner) {
        if (owner == null || !Boolean.TRUE.equals(owner.getActivo())) return false;
        SubscriptionStatus status = owner.getSubscriptionStatus();
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        if (status == SubscriptionStatus.TRIAL_ACTIVE) {
            return owner.getTrialEndDate() == null || !today.isAfter(owner.getTrialEndDate());
        }
        if (status == SubscriptionStatus.TRIALING) {
            return owner.getSubscriptionCurrentPeriodEnd() == null
                    || !owner.getSubscriptionCurrentPeriodEnd().isBefore(
                            LocalDateTime.now(clock.withZone(ZONE)));
        }
        return status == SubscriptionStatus.ACTIVE;
    }

    private SeguimientoPresupuestosResponse toResponse(Empresa empresa) {
        return new SeguimientoPresupuestosResponse(
                empresa.isSeguimientoActivo(), empresa.getSeguimientoDiasEspera(),
                empresa.getSeguimientoMaxAvisos(), empresa.isSeguimientoEmailResumen(),
                empresa.isPermitirRespuestaCliente());
    }

    private Digest digest(String locale, List<NewAlert> alerts) {
        String language = locale == null ? "es" : locale.split("[-_]")[0].toLowerCase();
        String subject = switch (language) {
            case "en" -> "Daily budget follow-up summary";
            case "fr" -> "Résumé quotidien du suivi des devis";
            case "ro" -> "Rezumat zilnic pentru urmărirea ofertelor";
            case "uk" -> "Щоденний підсумок нагадувань про кошториси";
            default -> "Resumen diario de seguimiento de presupuestos";
        };
        String heading = switch (language) {
            case "en" -> "Budgets that need follow-up:";
            case "fr" -> "Devis nécessitant un suivi :";
            case "ro" -> "Oferte care necesită urmărire:";
            case "uk" -> "Кошториси, які потребують уваги:";
            default -> "Presupuestos que necesitan seguimiento:";
        };
        String appLink = switch (language) {
            case "en" -> "Open the app";
            case "fr" -> "Ouvrir l’application";
            case "ro" -> "Deschide aplicația";
            case "uk" -> "Відкрити застосунок";
            default -> "Abrir la aplicación";
        };
        String disableHint = switch (language) {
            case "en" -> "You can turn off these summaries in Settings > Budget follow-up.";
            case "fr" -> "Vous pouvez désactiver ces résumés dans Paramètres > Suivi des devis.";
            case "ro" -> "Poți dezactiva aceste rezumate din Setări > Urmărirea ofertelor.";
            case "uk" -> "Ці підсумки можна вимкнути в Налаштуваннях > Нагадування про кошториси.";
            default -> "Puedes desactivar estos resúmenes en Configuración > Seguimiento de presupuestos.";
        };
        StringBuilder body = new StringBuilder("<p>").append(heading).append("</p><ul>");
        for (NewAlert alert : alerts) {
            String clientLabel = switch (language) {
                case "en" -> "Customer";
                case "fr" -> "Client";
                case "ro" -> "Client";
                case "uk" -> "Клієнт";
                default -> "Cliente";
            };
            String budgetLabel = switch (language) {
                case "en" -> "Estimate no.";
                case "fr" -> "Devis nº";
                case "ro" -> "Oferta nr.";
                case "uk" -> "Кошторис №";
                default -> "Presupuesto Nº";
            };
            String kind = switch (language) {
                case "en" -> alert.type.equals("NO_ABIERTO") ? "Not viewed" : "Viewed, no reply";
                case "fr" -> alert.type.equals("NO_ABIERTO") ? "Non consulté" : "Consulté, sans réponse";
                case "ro" -> alert.type.equals("NO_ABIERTO") ? "Nevizualizată" : "Vizualizată, fără răspuns";
                case "uk" -> alert.type.equals("NO_ABIERTO") ? "Не переглянуто" : "Переглянуто, без відповіді";
                default -> alert.type.equals("NO_ABIERTO") ? "No abierto" : "Abierto sin respuesta";
            };
            String days = switch (language) {
                case "en" -> alert.days + (alert.days == 1 ? " day" : " days");
                case "fr" -> alert.days + (alert.days == 1 ? " jour" : " jours");
                case "ro" -> alert.days + (alert.days == 1 ? " zi" : " zile");
                case "uk" -> alert.days + " дн.";
                default -> alert.days + (alert.days == 1 ? " día" : " días");
            };
            body.append("<li>").append(clientLabel).append(": ").append(escape(alert.clientName))
                    .append(" · ").append(budgetLabel).append(' ').append(alert.budgetId)
                    .append(" · ").append(kind).append(" · ").append(days).append("</li>");
        }
        String html = body.append("</ul><p><a href=\"").append(escape(frontendUrl))
                .append("/presupuestos\">").append(appLink).append("</a></p><p>")
                .append(disableHint).append("</p>").toString();
        return new Digest(subject, html);
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private record Digest(String subject, String body) {}
    private record NewAlert(Long budgetId, String clientName, String type, long days) {}
}
