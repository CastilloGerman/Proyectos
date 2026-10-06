package com.appgestion.api.service;

import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.exception.AiServiceException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Limita los intentos enviados al proveedor por usuario; el contador vive en esta instancia. */
@Component
public class AiProviderAttemptRateLimiter {

    private final GeminiProperties properties;
    private final Cache<Long, Window> windows = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(100_000)
            .build();

    public AiProviderAttemptRateLimiter(GeminiProperties properties) {
        this.properties = properties;
    }

    /** Cuenta cada intento HTTP, incluidos reintentos y respuestas fallidas. */
    public synchronized void checkAndRecord(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio para limitar las solicitudes de IA");
        }
        Instant now = Instant.now();
        Window window = windows.getIfPresent(userId);
        if (window == null || !now.isBefore(window.startedAt.plus(Duration.ofHours(1)))) {
            window = new Window(now);
        }
        if (window.count >= properties.getMaxProviderAttemptsPerHour()) {
            throw new AiServiceException(HttpStatus.TOO_MANY_REQUESTS,
                    "Has alcanzado el límite de intentos del servicio de IA por hora. Inténtalo más tarde.");
        }
        window.count++;
        windows.put(userId, window);
    }

    private static final class Window {
        private final Instant startedAt;
        private int count;

        private Window(Instant startedAt) { this.startedAt = startedAt; }
    }
}
