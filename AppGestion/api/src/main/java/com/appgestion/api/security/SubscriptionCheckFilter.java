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

@Component
public class SubscriptionCheckFilter extends OncePerRequestFilter {

    private final SubscriptionService subscriptionService;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.subscription.skip-check:false}")
    private boolean skipSubscriptionCheck;

    public SubscriptionCheckFilter(SubscriptionService subscriptionService, UsuarioRepository usuarioRepository) {
        this.subscriptionService = subscriptionService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (skipSubscriptionCheck || isExcludedPath(path)) {
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

        if (subscriptionService.hasActiveSubscription(usuario)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Suscripción requerida. Activa tu suscripción para acceder.\"}");
    }

    private boolean isExcludedPath(String path) {
        return path.contains("/auth/")
                || path.contains("/webhook/")
                || path.contains("/subscription/checkout")
                || path.contains("/subscription/portal");
    }
}
