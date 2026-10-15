package com.appgestion.api.security;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.SubscriptionService;
import com.appgestion.api.util.LogSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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

    // SEC-20: fail-closed. Si la propiedad falta, se EXIGE la suscripción (solo el perfil local la omite).
    @Value("${app.subscription.skip-check:false}")
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
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = normalizedPath(request);

        if (skipSubscriptionCheck || isExcludedPath(path) || isRevocarEnlace(request.getMethod(), path)) {
            if (path.contains("enviar-email")) {
                log.debug("SubscriptionCheckFilter: ruta excluida o skip-check activo - {}", path);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Las lecturas no comprueban suscripción. Evitar el SELECT de usuario aquí: tras el login
        // el dashboard dispara varias GET en paralelo y cada una competía por una conexión.
        // Deuda: GET /auth/audit-access/export inserta un evento de auditoría y pasa igual,
        // porque aquí se permite cualquier lectura.
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
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

        if (subscriptionService.canWrite(usuario)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("403 Forbidden: usuario {} en modo solo lectura - {} {}", LogSanitizer.maskEmail(email), method, LogSanitizer.clean(path));
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Cuenta en modo solo lectura. Activa tu suscripción para crear o editar.\"}");
    }

    /** Prefijos de ruta (tras normalizar) que se permiten aunque la cuenta este en modo solo lectura. */
    private static final java.util.List<String> EXCLUDED_PREFIXES = java.util.List.of(
            "/publico/presupuestos/",
            "/webhook/",
            "/subscription/checkout",
            "/subscription/portal",
            "/dev/",
            // El perfil (nombre/telefono) debe poder actualizarse aunque la suscripcion este en solo lectura.
            "/auth/profile",
            "/auth/account-settings",
            "/auth/preferences",
            "/auth/change-password",
            "/auth/totp",
            "/auth/support",
            "/auth/notifications",
            // Datos de empresa en PDFs: permitir guardar aunque la suscripcion este en solo lectura.
            "/config/empresa",
            // Conectar / desconectar OAuth de correo (afecta envio, no facturacion directa).
            "/auth/email/oauth");

    private static final java.util.regex.Pattern ENVIAR_EMAIL =
            java.util.regex.Pattern.compile("^/(presupuestos|facturas)/\\d+/enviar-email/?$");
    private static final java.util.regex.Pattern REVOCAR_ENLACE =
            java.util.regex.Pattern.compile("^/presupuestos/\\d+/enlace/?$");

    /**
     * SEC-29: la ruta se normaliza (sin context-path) y se compara por PREFIJO o por patron anclado, nunca con
     * {@code contains}: una ruta como {@code /clientes/x/auth/profile} ya no se salta la comprobacion.
     */
    public static String normalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (uri == null) {
            return "";
        }
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        return uri;
    }

    public static boolean isExcludedPath(String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return ENVIAR_EMAIL.matcher(path).matches();
    }

    public static boolean isRevocarEnlace(String method, String path) {
        return "DELETE".equalsIgnoreCase(method) && path != null && REVOCAR_ENLACE.matcher(path).matches();
    }
}