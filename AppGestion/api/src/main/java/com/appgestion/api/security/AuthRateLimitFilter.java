package com.appgestion.api.security;

import com.appgestion.api.util.TrustedClientAddressResolver;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Limita peticiones por IP en rutas de autenticación (fuerza bruta / abuso).
 * Se registra solo en {@link com.appgestion.api.config.SecurityConfig} (no como bean de filtro servlet suelto).
 *
 * Usa {@link TrustedClientAddressResolver} para resolver la IP del cliente según el modo configurado.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    /** SEC-11: tope de IPs rastreadas. Una entrada sin uso durante {@code refillMinutes} ya habría recuperado todo su cupo. */
    private static final long MAX_TRACKED_CLIENTS = 100_000;

    private final Cache<String, Bucket> cache;
    private final boolean enabled;
    private final int capacity;
    private final int refillMinutes;
    private final TrustedClientAddressResolver.ClientIpMode mode;
    private final List<TrustedClientAddressResolver.Cidr> trustedProxies;

    public AuthRateLimitFilter(boolean enabled, int capacity, int refillMinutes,
                               TrustedClientAddressResolver.ClientIpMode mode,
                               List<TrustedClientAddressResolver.Cidr> trustedProxies) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillMinutes = refillMinutes;
        this.mode = mode;
        this.trustedProxies = trustedProxies;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(Math.max(1, refillMinutes) + 1L))
                .maximumSize(MAX_TRACKED_CLIENTS)
                .build();
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
        Bucket bucket = cache.get(key, k -> newBucket());
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
        if (uri == null) {
            return false;
        }
        // SEC-11: la comprobación de un token de invitación es un GET que permite sondear tokens.
        if ("GET".equalsIgnoreCase(method)) {
            return uri.endsWith("/auth/invite/verify");
        }
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return uri.endsWith("/auth/login")
                || uri.endsWith("/auth/register")
                || uri.endsWith("/auth/google")
                || uri.endsWith("/auth/forgot-password")
                || uri.endsWith("/auth/reset-password")
                || uri.endsWith("/auth/verify-email")
                || uri.endsWith("/auth/resend-verification")
                || uri.endsWith("/auth/invite/accept")
                // Endpoints autenticados que comprueban una credencial (contraseña o TOTP).
                || uri.endsWith("/auth/change-password")
                || uri.endsWith("/auth/totp/disable")
                || uri.endsWith("/auth/totp/setup/confirm");
    }

    private Bucket newBucket() {
        Duration period = Duration.ofMinutes(Math.max(1, refillMinutes));
        return Bucket.builder()
                .addLimit(l -> l.capacity(capacity).refillIntervally(capacity, period))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        return TrustedClientAddressResolver.resolveStatic(request, mode, trustedProxies);
    }
}
