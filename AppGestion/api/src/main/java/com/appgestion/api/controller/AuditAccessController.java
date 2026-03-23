package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.AuditAccessEventType;
import com.appgestion.api.dto.response.AuditAccessPageResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.service.AuditAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@RestController
@RequestMapping("/auth/audit-access")
public class AuditAccessController {

    private final AuditAccessService auditAccessService;
    private final UsuarioRepository usuarioRepository;

    public AuditAccessController(AuditAccessService auditAccessService, UsuarioRepository usuarioRepository) {
        this.auditAccessService = auditAccessService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public AuditAccessPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) AuditAccessEventType eventType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long usuarioId) {
        Usuario current = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return auditAccessService.listForCurrentUser(current, page, size, from, to, eventType, success, ip, q, usuarioId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) AuditAccessEventType eventType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long usuarioId,
            HttpServletRequest request) {
        Usuario current = SecurityUtils.getCurrentUsuario(usuarioRepository);
        byte[] body = auditAccessService.export(current, format, from, to, eventType, success, ip, q, usuarioId, request);
        String ext = "json".equalsIgnoreCase(format) ? "json" : "csv";
        String mime = "json".equalsIgnoreCase(format) ? MediaType.APPLICATION_JSON_VALUE : "text/csv";
        String filename = "historial-accesos." + ext;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(mime + ";charset=UTF-8"))
                .body(body);
    }
}
