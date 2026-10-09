package com.appgestion.api.controller;

import com.appgestion.api.dto.response.PresupuestoPublicoResponse;
import com.appgestion.api.service.PublicBudgetLinkService;
import com.appgestion.api.service.PublicLinkRateLimiter;
import com.appgestion.api.service.PublicClientAddressResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/publico/presupuestos")
public class PresupuestoPublicoController {
    private static final String UNAVAILABLE = "Este enlace no está disponible";
    private final PublicBudgetLinkService linkService;
    private final PublicLinkRateLimiter rateLimiter;
    private final PublicClientAddressResolver clientAddressResolver;

    public PresupuestoPublicoController(PublicBudgetLinkService linkService, PublicLinkRateLimiter rateLimiter,
                                        PublicClientAddressResolver clientAddressResolver) {
        this.linkService = linkService;
        this.rateLimiter = rateLimiter;
        this.clientAddressResolver = clientAddressResolver;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> get(@PathVariable String token, HttpServletRequest request) {
        ResponseEntity<?> limited = rateLimit(token, request);
        if (limited != null) return limited;
        try {
            return ResponseEntity.ok().headers(publicHeaders()).body(linkService.getPublic(token));
        } catch (ResponseStatusException ex) {
            return unavailable();
        }
    }

    @GetMapping(value = "/{token}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> pdf(@PathVariable String token, HttpServletRequest request) {
        ResponseEntity<?> limited = rateLimit(token, request);
        if (limited != null) return limited;
        try {
            return ResponseEntity.ok().headers(publicHeaders())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"presupuesto.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF).body(linkService.getPdf(token));
        } catch (ResponseStatusException ex) {
            return unavailable();
        }
    }

    @PostMapping("/{token}/visto")
    public ResponseEntity<?> viewed(@PathVariable String token, HttpServletRequest request) {
        ResponseEntity<?> limited = rateLimit(token, request);
        if (limited != null) return limited;
        try {
            linkService.registerView(token);
            return ResponseEntity.noContent().headers(publicHeaders()).build();
        } catch (ResponseStatusException ex) {
            return unavailable();
        }
    }

    private ResponseEntity<?> rateLimit(String token, HttpServletRequest request) {
        if (rateLimiter.allow(clientAddressResolver.resolve(request), PublicBudgetLinkService.hash(token))) return null;
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(publicHeaders())
                .header(HttpHeaders.RETRY_AFTER, "60")
                .body(Map.of("error", "Demasiadas solicitudes. Inténtalo más tarde."));
    }

    private ResponseEntity<?> unavailable() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(publicHeaders())
                .contentType(MediaType.APPLICATION_JSON).body(Map.of("error", UNAVAILABLE));
    }

    private HttpHeaders publicHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("no-store");
        headers.set("X-Robots-Tag", "noindex, nofollow");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("X-Content-Type-Options", "nosniff");
        return headers;
    }
}
