package com.appgestion.api.service;

import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.exception.AiServiceException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Limitador local por usuario; cada ventana fija dura una hora. */
@Component
public class AiRequestRateLimiter {

    private final GeminiProperties properties;
    private final Cache<Long, Window> windows = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(100_000)
            .build();

    public AiRequestRateLimiter(GeminiProperties properties) {
        this.properties = properties;
    }

    public synchronized void checkAndRecord(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio para limitar las solicitudes de IA");
        }
        Instant now = Instant.now();
        Window window = windows.getIfPresent(userId);
        if (window == null || now.isAfter(window.startedAt().plus(Duration.ofHours(1)))) {
            windows.put(userId, new Window(now, 1));
            return;
        }
        if (window.count() >= properties.getRequestsPerHour()) {
            throw new AiServiceException(HttpStatus.TOO_MANY_REQUESTS,
                    "Has alcanzado el límite de solicitudes de IA por hora. Inténtalo más tarde.");
        }
        windows.put(userId, new Window(window.startedAt(), window.count() + 1));
    }

    private record Window(Instant startedAt, int count) {}
}
