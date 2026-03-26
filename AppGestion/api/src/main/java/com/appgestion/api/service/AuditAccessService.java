package com.appgestion.api.service;

import com.appgestion.api.domain.entity.AuditAccessEvent;
import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.AuditAccessEventType;
import com.appgestion.api.dto.response.AuditAccessEventResponse;
import com.appgestion.api.dto.response.AuditAccessPageResponse;
import com.appgestion.api.repository.AuditAccessEventRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.util.ClientMetadataParser;
import com.appgestion.api.util.IpAnonymizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuditAccessService {

    private static final Logger log = LoggerFactory.getLogger(AuditAccessService.class);

    private static final int MAX_UA_LEN = 512;
    private static final int MAX_METADATA_JSON = 4000;
    private static final int EXPORT_MAX_ROWS = 5000;

    private final AuditAccessEventRepository auditAccessEventRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.audit.access.anonymize-ip:true}")
    private boolean anonymizeIp;

    public AuditAccessService(AuditAccessEventRepository auditAccessEventRepository,
                              UsuarioRepository usuarioRepository,
                              ObjectMapper objectMapper) {
        this.auditAccessEventRepository = auditAccessEventRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistNew(@NonNull AuditAccessEvent event) {
        auditAccessEventRepository.save(Objects.requireNonNull(event, "event"));
    }

    /**
     * Inserta un evento de auditoría sin afectar la transacción principal (p. ej. login fallido).
     */
    public void recordSafe(Runnable insert) {
        try {
            insert.run();
        } catch (Exception e) {
            log.warn("Registro de auditoría omitido: {}", e.getMessage());
        }
    }

    public void recordForUsuario(Usuario usuario, AuditAccessEventType type, boolean success, String failureReason,
                                 boolean sensitive, HttpServletRequest request, String sessionId, String resourcePath,
                                 Map<String, Object> metadata) {
        recordSafe(() -> {
            AuditAccessEvent e = buildEvent(usuario, type, success, failureReason, sensitive, request, sessionId,
                    resourcePath, metadata);
            persistNew(e);
        });
    }

    /** Registro sin petición HTTP (p. ej. tests o tareas internas). */
    public void recordForUsuario(Usuario usuario, AuditAccessEventType type, boolean success, String failureReason,
                                 boolean sensitive, String sessionId, String resourcePath, Map<String, Object> metadata) {
        recordForUsuario(usuario, type, success, failureReason, sensitive, null, sessionId, resourcePath, metadata);
    }

    public void recordExport(Usuario actor, HttpServletRequest request, AuditAccessEventType exportType, String format) {
        recordForUsuario(actor, exportType, true, null, true, request, null, "/auth/audit-access/export",
                Map.of("format", format));
    }

    private @NonNull AuditAccessEvent buildEvent(Usuario usuario, AuditAccessEventType type, boolean success, String failureReason,
                                        boolean sensitive, HttpServletRequest request, String sessionId,
                                        String resourcePath, Map<String, Object> metadata) {
        Organization org = usuario.getOrganization();
        if (org == null) {
            throw new IllegalStateException("Usuario sin organización");
        }
        ClientMetadataParser.Snapshot snap = request == null
                ? new ClientMetadataParser.Snapshot("", "", "Desconocido", "Desconocido", "UNKNOWN", "—")
                : ClientMetadataParser.parse(request, null);
        String rawIp = snap.ipAddress();
        boolean anon = anonymizeIp && StringUtils.hasText(rawIp);
        String ip = anon ? IpAnonymizer.anonymize(rawIp) : rawIp;
        if (ip != null && ip.length() > 45) {
            ip = ip.substring(0, 45);
        }
        String ua = snap.userAgent();
        if (ua != null && ua.length() > MAX_UA_LEN) {
            ua = ua.substring(0, MAX_UA_LEN);
        }
        AuditAccessEvent e = new AuditAccessEvent();
        e.setOrganization(org);
        e.setUsuario(usuario);
        e.setActorEmail(usuario.getEmail());
        e.setOccurredAt(Instant.now());
        e.setEventType(type);
        e.setSuccess(success);
        e.setFailureReason(truncate(failureReason, 255));
        e.setIpAddress(ip);
        e.setIpAnonymized(anon);
        e.setUserAgent(ua);
        e.setCountryCode(request != null ? readCountryHint(request) : null);
        e.setSessionId(truncate(sessionId, 40));
        e.setResourcePath(truncate(resourcePath, 512));
        e.setTraceId(request != null ? truncate(readTraceId(request), 64) : null);
        e.setSensitive(sensitive);
        e.setMetadataJson(serializeMetadata(metadata));
        return e;
    }

    private static String readCountryHint(HttpServletRequest request) {
        String cc = request.getHeader("CF-IPCountry");
        if (!StringUtils.hasText(cc) || cc.length() != 2) {
            return null;
        }
        return cc.trim().toUpperCase();
    }

    private static String readTraceId(HttpServletRequest request) {
        String t = request.getHeader("X-Request-Id");
        if (!StringUtils.hasText(t)) {
            t = request.getHeader("X-Correlation-Id");
        }
        return t != null ? t.trim() : null;
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(metadata);
            if (json.length() > MAX_METADATA_JSON) {
                return json.substring(0, MAX_METADATA_JSON) + "…";
            }
            return json;
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"metadata_serialización\"}";
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Transactional(readOnly = true)
    public AuditAccessPageResponse listForCurrentUser(Usuario current, int page, int size, Instant from, Instant to,
                                                      AuditAccessEventType eventType, Boolean successFilter,
                                                      String ipContains, String q, Long filterUsuarioId) {
        boolean admin = isApplicationAdmin(current);
        if (admin && filterUsuarioId != null) {
            Usuario other = usuarioRepository.findById(filterUsuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            if (!other.getOrganization().getId().equals(current.getOrganization().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario fuera de tu organización");
            }
        }

        Specification<AuditAccessEvent> spec = buildSpec(current, admin, filterUsuarioId, from, to, eventType,
                successFilter, ipContains, q);
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        Page<AuditAccessEvent> result = auditAccessEventRepository.findAll(spec,
                PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "occurredAt")));

        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        long failed24h = auditAccessEventRepository.count(
                spec.and((root, query, cb) -> cb.isFalse(root.get("success")))
                        .and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), since24h)));
        long sensitive24h = auditAccessEventRepository.count(
                spec.and((root, query, cb) -> cb.isTrue(root.get("sensitive")))
                        .and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), since24h)));

        List<AuditAccessEventResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return new AuditAccessPageResponse(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                failed24h,
                sensitive24h
        );
    }

    private static boolean isApplicationAdmin(Usuario u) {
        return u.getRol() != null && "ADMIN".equalsIgnoreCase(u.getRol().trim());
    }

    private Specification<AuditAccessEvent> buildSpec(Usuario current, boolean admin, Long filterUsuarioId,
                                                        Instant from, Instant to, AuditAccessEventType eventType,
                                                        Boolean successFilter, String ipContains, String q) {
        Long orgId = current.getOrganization().getId();
        return (root, query, cb) -> {
            List<Predicate> parts = new ArrayList<>();
            parts.add(cb.equal(root.get("organization").get("id"), orgId));
            if (!admin) {
                parts.add(cb.equal(root.get("usuario").get("id"), current.getId()));
            } else if (filterUsuarioId != null) {
                parts.add(cb.equal(root.get("usuario").get("id"), filterUsuarioId));
            }
            if (from != null) {
                parts.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                parts.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            if (eventType != null) {
                parts.add(cb.equal(root.get("eventType"), eventType));
            }
            if (successFilter != null) {
                parts.add(cb.equal(root.get("success"), successFilter));
            }
            if (StringUtils.hasText(ipContains)) {
                String lit = "%" + ipContains.trim() + "%";
                parts.add(cb.like(root.get("ipAddress"), lit));
            }
            if (StringUtils.hasText(q)) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                parts.add(cb.or(
                        cb.like(cb.lower(root.get("actorEmail")), pattern),
                        cb.like(cb.lower(root.get("resourcePath")), pattern),
                        cb.like(cb.lower(root.get("failureReason")), pattern)
                ));
            }
            return cb.and(parts.toArray(Predicate[]::new));
        };
    }

    private AuditAccessEventResponse toResponse(AuditAccessEvent e) {
        String email = e.getUsuario() != null ? e.getUsuario().getEmail() : e.getActorEmail();
        return new AuditAccessEventResponse(
                e.getId(),
                e.getOccurredAt(),
                e.getUsuario() != null ? e.getUsuario().getId() : null,
                email,
                e.getEventType().name(),
                e.isSuccess(),
                e.getFailureReason(),
                e.getIpAddress(),
                e.getUserAgent(),
                e.getCountryCode(),
                e.getSessionId(),
                e.getResourcePath(),
                e.getTraceId(),
                e.isSensitive(),
                e.getMetadataJson()
        );
    }

    @Transactional(readOnly = true)
    public byte[] export(Usuario current, String format, Instant from, Instant to, AuditAccessEventType eventType,
                         Boolean successFilter, String ipContains, String q, Long filterUsuarioId,
                         HttpServletRequest request) {
        boolean admin = isApplicationAdmin(current);
        if (admin && filterUsuarioId != null) {
            Usuario other = usuarioRepository.findById(filterUsuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            if (!other.getOrganization().getId().equals(current.getOrganization().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario fuera de tu organización");
            }
        }
        Specification<AuditAccessEvent> spec = buildSpec(current, admin, filterUsuarioId, from, to, eventType,
                successFilter, ipContains, q);
        Page<AuditAccessEvent> page = auditAccessEventRepository.findAll(spec,
                PageRequest.of(0, EXPORT_MAX_ROWS, Sort.by(Sort.Direction.DESC, "occurredAt")));
        List<AuditAccessEventResponse> rows = page.getContent().stream().map(this::toResponse).toList();

        if ("json".equalsIgnoreCase(format)) {
            recordExport(current, request, AuditAccessEventType.AUDIT_EXPORT_JSON, "json");
            try {
                return objectMapper.writeValueAsString(rows).getBytes(StandardCharsets.UTF_8);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Export JSON fallido", e);
            }
        }
        recordExport(current, request, AuditAccessEventType.AUDIT_EXPORT_CSV, "csv");
        return buildCsv(rows);
    }

    private static byte[] buildCsv(List<AuditAccessEventResponse> rows) {
        String header = "id,occurredAt,userEmail,eventType,success,failureReason,ip,country,sessionId,resource,sensitive,metadata\n";
        StringBuilder sb = new StringBuilder(header);
        for (AuditAccessEventResponse r : rows) {
            sb.append(r.id()).append(',')
                    .append(csv(r.occurredAt())).append(',')
                    .append(csv(r.userEmail())).append(',')
                    .append(csv(r.eventType())).append(',')
                    .append(r.success()).append(',')
                    .append(csv(r.failureReason())).append(',')
                    .append(csv(r.ipAddress())).append(',')
                    .append(csv(r.countryCode())).append(',')
                    .append(csv(r.sessionId())).append(',')
                    .append(csv(r.resourcePath())).append(',')
                    .append(r.sensitive()).append(',')
                    .append(csv(r.metadataJson()))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(Object v) {
        if (v == null) {
            return "";
        }
        String s = String.valueOf(v).replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s + "\"";
        }
        return s;
    }

}
