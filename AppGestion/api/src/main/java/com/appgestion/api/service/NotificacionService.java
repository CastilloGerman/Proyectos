package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Notificacion;
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

    @Transactional(readOnly = true)
    public Page<NotificacionResponse> listForCurrentUser(Long usuarioId, Boolean readFilter, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(50, Math.max(1, size));
        var pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
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
