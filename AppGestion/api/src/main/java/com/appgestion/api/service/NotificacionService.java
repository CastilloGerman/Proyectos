package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Notificacion;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.NotificacionSeveridad;
import com.appgestion.api.domain.enums.NotificacionTipo;
import com.appgestion.api.dto.response.NotificacionResponse;
import com.appgestion.api.repository.NotificacionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Si el usuario no tiene ninguna notificación, crea un aviso de bienvenida (idempotente por conteo).
     */
    @Transactional
    public void ensureWelcomeIfEmpty(Long usuarioId) {
        Long uid = Objects.requireNonNull(usuarioId, "usuarioId");
        if (notificacionRepository.countByUsuarioId(uid) > 0) {
            return;
        }

        Notificacion n = new Notificacion();
        n.setUsuario(usuarioRepository.getReferenceById(uid));
        n.setTipo(NotificacionTipo.SISTEMA);
        n.setSeveridad(NotificacionSeveridad.INFO);
        n.setTitulo("Bienvenido a tu centro de notificaciones");
        n.setResumen(
                "Aquí verás avisos importantes: estado de tu suscripción o prueba, facturas de clientes vencidas y "
                        + "novedades del producto. También puedes revisarlas desde la campana del menú superior.");
        n.setLeida(false);
        n.setActionPath("/cuenta/suscripcion");
        notificacionRepository.save(n);
    }

    @Transactional
    public void presupuestoVisto(Usuario owner, String clientName, Long budgetNumber) {
        String locale = owner.getUiLocale();
        String language = locale == null ? "es" : locale.split("[-_]")[0];
        Notificacion notification = new Notificacion();
        notification.setUsuario(owner);
        notification.setTipo(NotificacionTipo.SISTEMA);
        notification.setSeveridad(NotificacionSeveridad.INFO);
        notification.setTitulo(switch (language) {
            case "en" -> "Estimate viewed";
            case "fr" -> "Devis consulté";
            case "ro" -> "Ofertă vizualizată";
            case "uk" -> "Кошторис переглянуто";
            default -> "Presupuesto visto";
        });
        notification.setResumen(switch (language) {
            case "en" -> "Your customer " + clientName + " viewed estimate no. " + budgetNumber;
            case "fr" -> "Votre client " + clientName + " a consulté le devis nº " + budgetNumber;
            case "ro" -> "Clientul " + clientName + " a văzut oferta nr. " + budgetNumber;
            case "uk" -> "Ваш клієнт " + clientName + " переглянув кошторис № " + budgetNumber;
            default -> "Tu cliente " + clientName + " ha visto el presupuesto Nº " + budgetNumber;
        });
        notification.setActionPath("/presupuestos/" + budgetNumber);
        notificacionRepository.save(notification);
    }

    @Transactional
    public void presupuestoSeguimiento(Usuario owner, String clientName, Long budgetNumber, String tipo, long days) {
        String locale = owner.getUiLocale();
        String language = locale == null ? "es" : locale.split("[-_]")[0].toLowerCase();
        boolean viewed = "ABIERTO_SIN_RESPUESTA".equals(tipo);
        String title = switch (language) {
            case "en" -> viewed ? "Estimate awaiting your follow-up" : "Estimate not viewed yet";
            case "fr" -> viewed ? "Devis consulté, sans réponse" : "Devis pas encore consulté";
            case "ro" -> viewed ? "Oferta vizualizată, fără răspuns" : "Oferta nu a fost încă vizualizată";
            case "uk" -> viewed ? "Кошторис переглянуто, відповіді немає" : "Кошторис ще не переглянули";
            default -> viewed ? "Presupuesto visto, sin respuesta" : "Presupuesto aún no visto";
        };
        String summary = switch (language) {
            case "en" -> "Your customer " + clientName + " has not replied to estimate no. " + budgetNumber
                    + " after " + days + " days.";
            case "fr" -> "Votre client " + clientName + " n’a pas répondu au devis nº " + budgetNumber
                    + " après " + days + " jours.";
            case "ro" -> "Clientul " + clientName + " nu a răspuns la oferta nr. " + budgetNumber
                    + " după " + days + " zile.";
            case "uk" -> "Клієнт " + clientName + " не відповів на кошторис № " + budgetNumber
                    + " протягом " + days + " днів.";
            default -> "Tu cliente " + clientName + " no ha respondido al presupuesto Nº " + budgetNumber
                    + " después de " + days + " días.";
        };
        Notificacion notification = new Notificacion();
        notification.setUsuario(owner);
        notification.setTipo(NotificacionTipo.SISTEMA);
        notification.setSeveridad(NotificacionSeveridad.INFO);
        notification.setTitulo(title);
        notification.setResumen(summary);
        notification.setActionPath("/presupuestos/" + budgetNumber);
        notification.setLeida(false);
        notificacionRepository.save(notification);
    }

    @Transactional
    public void respuestaClientePresupuesto(Usuario owner, String clientName, Long budgetNumber, String opcion) {
        String locale = owner.getUiLocale();
        String language = locale == null ? "es" : locale.split("[-_]")[0].toLowerCase();
        boolean interested = "INTERESA".equals(opcion);
        String title = switch (language) {
            case "en" -> "Your customer replied";
            case "fr" -> "Votre client a répondu";
            case "ro" -> "Clientul a răspuns";
            case "uk" -> "Клієнт відповів";
            default -> "Tu cliente ha respondido";
        };
        String summary = switch (language) {
            case "en" -> clientName + (interested ? " is interested in estimate no. " : " has questions about estimate no. ") + budgetNumber;
            case "fr" -> clientName + (interested ? " est intéressé par le devis nº " : " a des questions sur le devis nº ") + budgetNumber;
            case "ro" -> clientName + (interested ? " este interesat de oferta nr. " : " are întrebări despre oferta nr. ") + budgetNumber;
            case "uk" -> clientName + (interested ? " зацікавлений у кошторисі № " : " має запитання щодо кошторису № ") + budgetNumber;
            default -> clientName + (interested ? " ha indicado que le interesa el presupuesto Nº " : " tiene dudas sobre el presupuesto Nº ") + budgetNumber;
        };
        Notificacion notification = new Notificacion();
        notification.setUsuario(owner);
        notification.setTipo(NotificacionTipo.SISTEMA);
        notification.setSeveridad(NotificacionSeveridad.INFO);
        notification.setTitulo(title);
        notification.setResumen(summary);
        notification.setActionPath("/presupuestos/" + budgetNumber);
        notification.setLeida(false);
        notificacionRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificacionResponse> listForCurrentUser(Long usuarioId, Boolean readFilter, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(50, Math.max(1, size));
        var pageable = PageRequest.of(p, s, Sort.by((Notificacion n) -> n.getCreatedAt()).descending());
        return notificacionRepository.findForUsuario(usuarioId, readFilter, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long usuarioId) {
        return notificacionRepository.countByUsuarioIdAndLeidaIsFalse(usuarioId);
    }

    @Transactional
    public void markRead(Long usuarioId, Long notificacionId) {
        Notificacion n = notificacionRepository
                .findByIdAndUsuario_Id(notificacionId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada"));
        if (!n.isLeida()) {
            n.setLeida(true);
            notificacionRepository.save(n);
        }
    }

    @Transactional
    public int markAllRead(Long usuarioId) {
        return notificacionRepository.markAllReadForUsuario(usuarioId);
    }

    private NotificacionResponse toResponse(Notificacion n) {
        return new NotificacionResponse(
                n.getId(),
                n.getTipo(),
                n.getSeveridad(),
                n.getTitulo(),
                n.getResumen(),
                n.isLeida(),
                sanitizeActionPath(n.getActionPath()),
                n.getCreatedAt()
        );
    }

    /**
     * Solo rutas relativas internas; evita open redirect.
     */
    static String sanitizeActionPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String p = path.trim();
        if (p.startsWith("//") || p.contains("://")) {
            return null;
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p.length() > 500 ? p.substring(0, 500) : p;
    }
}
