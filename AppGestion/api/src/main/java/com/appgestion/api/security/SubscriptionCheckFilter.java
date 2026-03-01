package com.appgestion.api.security;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SubscriptionCheckFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCheckFilter.class);

    private final SubscriptionService subscriptionService;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.subscription.skip-check:true}")
    private boolean skipSubscriptionCheck;

    public SubscriptionCheckFilter(SubscriptionService subscriptionService, UsuarioRepository usuarioRepository) {
        this.subscriptionService = subscriptionService;
        this.usuarioRepository = usuarioRepository;
    }

    @jakarta.annotation.PostConstruct
    public void logConfig() {
        log.info("SubscriptionCheckFilter: skip-check={} (true=omitir suscripción, false=exigir suscripción activa)", skipSubscriptionCheck);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (skipSubscriptionCheck || isExcludedPath(path)) {
            if (path.contains("enviar-email")) {
                log.debug("SubscriptionCheckFilter: ruta excluida o skip-check activo - {}", path);
            }
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (subscriptionService.canWrite(usuario)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("403 Forbidden: usuario {} en modo solo lectura - {} {}", email, method, path);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Cuenta en modo solo lectura. Activa tu suscripción para crear o editar.\"}");
    }

    private boolean isExcludedPath(String path) {
        return path.contains("/auth/")
                || path.contains("/webhook/")
                || path.contains("/subscription/checkout")
                || path.contains("/subscription/portal")
                || path.contains("/enviar-email");
    }
}
