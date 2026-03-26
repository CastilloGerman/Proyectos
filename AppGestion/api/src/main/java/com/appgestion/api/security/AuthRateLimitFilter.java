package com.appgestion.api.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita peticiones por IP en rutas de autenticación (fuerza bruta / abuso).
 * Se registra solo en {@link com.appgestion.api.config.SecurityConfig} (no como bean de filtro servlet suelto).
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final boolean enabled;
    private final int capacity;
    private final int refillMinutes;

    public AuthRateLimitFilter(boolean enabled, int capacity, int refillMinutes) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillMinutes = refillMinutes;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || !isAuthRateLimitedPath(request.getRequestURI(), request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request);
        Bucket bucket = cache.computeIfAbsent(key, k -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Demasiados intentos. Espera unos minutos e inténtalo de nuevo.\"}");
        }
    }

    private boolean isAuthRateLimitedPath(String uri, String method) {
        if (uri == null || !"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return uri.endsWith("/auth/login")
                || uri.endsWith("/auth/register")
                || uri.endsWith("/auth/google")
                || uri.endsWith("/auth/forgot-password")
                || uri.endsWith("/auth/reset-password")
                || uri.endsWith("/auth/invite/accept");
    }

    private Bucket newBucket() {
        Duration period = Duration.ofMinutes(Math.max(1, refillMinutes));
        return Bucket.builder()
                .addLimit(l -> l.capacity(capacity).refillIntervally(capacity, period))
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
