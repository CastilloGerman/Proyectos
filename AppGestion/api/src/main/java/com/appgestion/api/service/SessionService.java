package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.entity.UsuarioSesion;
import com.appgestion.api.dto.request.DeviceClientInfoRequest;
import com.appgestion.api.dto.response.SesionDispositivoResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.repository.UsuarioSesionRepository;
import com.appgestion.api.security.JwtService;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.util.ClientMetadataParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private static final int UA_PREVIEW_LEN = 120;
    private static final Duration ACTIVITY_UPDATE_INTERVAL = Duration.ofMinutes(5);

    private final UsuarioSesionRepository sesionRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public SessionService(UsuarioSesionRepository sesionRepository,
                          UsuarioRepository usuarioRepository,
                          JwtService jwtService) {
        this.sesionRepository = sesionRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public UsuarioSesion createSession(Usuario usuario, HttpServletRequest request, DeviceClientInfoRequest clientInfo) {
        ClientMetadataParser.Snapshot snap = ClientMetadataParser.parse(request, clientInfo);
        Instant now = Instant.now();
        UsuarioSesion s = new UsuarioSesion();
        s.setId(UUID.randomUUID().toString());
        s.setUsuario(usuario);
        s.setCreatedAt(now);
        s.setLastActivityAt(now);
        s.setExpiresAt(now.plusMillis(expirationMs));
        s.setIpAddress(snap.ipAddress());
        s.setUserAgent(snap.userAgent());
        s.setBrowser(snap.browser());
        s.setOsName(snap.osName());
        s.setDeviceType(snap.deviceType());
        s.setClientLabel(snap.displayLabel());
        return sesionRepository.save(s);
    }

    /**
     * Valida sesión activa para el JWT con claim {@code sid}. Actualiza última actividad con throttling.
     *
     * @return vacío si la sesión no es válida
     */
    @Transactional
    public Optional<UsuarioSesion> validateAndTouchSession(String sessionId, String emailFromJwt) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(emailFromJwt)) {
            return Optional.empty();
        }
        Optional<UsuarioSesion> opt = sesionRepository.findByIdWithUsuario(sessionId.trim());
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        UsuarioSesion s = opt.get();
        if (s.getRevokedAt() != null || !s.getExpiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        String email = s.getUsuario().getEmail();
        if (email == null || !email.trim().equalsIgnoreCase(emailFromJwt.trim())) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        if (Duration.between(s.getLastActivityAt(), now).compareTo(ACTIVITY_UPDATE_INTERVAL) >= 0) {
            s.setLastActivityAt(now);
            sesionRepository.save(s);
        }
        return Optional.of(s);
    }

    @Transactional(readOnly = true)
    public List<SesionDispositivoResponse> listForCurrentUser(HttpServletRequest httpRequest) {
        Usuario u = SecurityUtils.getCurrentUsuario(usuarioRepository);
        String token = extractBearer(httpRequest);
        String currentSid = token != null ? jwtService.extractSessionId(token).orElse(null) : null;
        return listForUser(u.getId(), currentSid);
    }

    private List<SesionDispositivoResponse> listForUser(Long usuarioId, String currentSessionId) {
        List<UsuarioSesion> all = sesionRepository.findByUsuarioIdOrderByLastActivityAtDesc(usuarioId);
        Instant now = Instant.now();
        return all.stream()
                .filter(s -> s.getRevokedAt() == null && s.getExpiresAt().isAfter(now))
                .sorted(Comparator.comparing(UsuarioSesion::getLastActivityAt).reversed())
                .map(s -> toDto(s, currentSessionId, now))
                .toList();
    }

    private SesionDispositivoResponse toDto(UsuarioSesion s, String currentSessionId, Instant now) {
        String ua = s.getUserAgent();
        String preview = "";
        if (StringUtils.hasText(ua)) {
            preview = ua.length() <= UA_PREVIEW_LEN ? ua : ua.substring(0, UA_PREVIEW_LEN) + "…";
        }
        boolean current = currentSessionId != null && currentSessionId.equals(s.getId());
        boolean active = s.getRevokedAt() == null && s.getExpiresAt().isAfter(now);
        return new SesionDispositivoResponse(
                s.getId(),
                s.getCreatedAt(),
                s.getLastActivityAt(),
                s.getExpiresAt(),
                s.getIpAddress(),
                s.getBrowser(),
                s.getOsName(),
                s.getDeviceType(),
                s.getClientLabel(),
                preview,
                current && active
        );
    }

    @Transactional
    public void revokeSession(String sessionId, HttpServletRequest httpRequest) {
        Usuario u = SecurityUtils.getCurrentUsuario(usuarioRepository);
        String token = extractBearer(httpRequest);
        String currentSid = token != null ? jwtService.extractSessionId(token).orElse(null) : null;
        if (sessionId != null && sessionId.equals(currentSid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para cerrar esta sesión usa «Cerrar sesión» en el menú de usuario.");
        }
        UsuarioSesion s = sesionRepository.findByIdWithUsuario(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesión no encontrada"));
        if (!s.getUsuario().getId().equals(u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes cerrar esta sesión");
        }
        if (s.getRevokedAt() == null) {
            s.setRevokedAt(Instant.now());
            sesionRepository.save(s);
        }
    }

    /**
     * Revoca la sesión del token actual (logout servidor).
     */
    @Transactional
    public void revokeCurrentSession(HttpServletRequest httpRequest) {
        Usuario u = SecurityUtils.getCurrentUsuario(usuarioRepository);
        String token = extractBearer(httpRequest);
        if (token == null) {
            return;
        }
        jwtService.extractSessionId(token).flatMap(sesionRepository::findById).ifPresent(s -> {
            if (s.getUsuario().getId().equals(u.getId()) && s.getRevokedAt() == null) {
                s.setRevokedAt(Instant.now());
                sesionRepository.save(s);
            }
        });
    }

    @Transactional
    public int revokeOtherSessions(HttpServletRequest httpRequest) {
        Usuario u = SecurityUtils.getCurrentUsuario(usuarioRepository);
        String token = extractBearer(httpRequest);
        String currentSid = token != null ? jwtService.extractSessionId(token).orElse(null) : null;
        if (!StringUtils.hasText(currentSid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tu sesión actual no incluye identificador; inicia sesión de nuevo.");
        }
        return sesionRepository.revokeAllForUserExcept(u.getId(), currentSid, Instant.now());
    }

    private static String extractBearer(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7).trim();
        }
        return null;
    }
}
