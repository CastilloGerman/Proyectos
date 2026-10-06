package com.appgestion.api.service;

import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.exception.AiServiceException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Cuota diaria local por usuario. No sincroniza contadores entre instancias. */
@Component
public class AiDailyRequestRateLimiter {

    private static final Duration DAY = Duration.ofDays(1);
    private final GeminiProperties properties;
    private final Cache<Long, Window> windows = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(25))
            .maximumSize(100_000)
            .build();

    public AiDailyRequestRateLimiter(GeminiProperties properties) {
        this.properties = properties;
    }

    public synchronized void checkAndRecord(Long userId) {
        if (userId == null) throw new IllegalArgumentException("El usuario es obligatorio");
        Instant now = Instant.now();
        Window window = currentWindow(userId, now);
        if (window.count + window.inFlight >= properties.getPresupuestoRequestsPerDay()) {
            throw new AiServiceException(HttpStatus.TOO_MANY_REQUESTS,
                    "Has alcanzado el límite diario de borradores de presupuesto con IA. Inténtalo mañana.");
        }
        window.count++;
        windows.put(userId, window);
    }

    public synchronized AiRequestQuotaPermit reserve(Long userId) {
        if (userId == null) throw new IllegalArgumentException("El usuario es obligatorio");
        Window window = currentWindow(userId, Instant.now());
        if (window.count + window.inFlight >= properties.getPresupuestoRequestsPerDay()) {
            throw new AiServiceException(HttpStatus.TOO_MANY_REQUESTS,
                    "Has alcanzado el límite diario de borradores de presupuesto con IA. Inténtalo mañana.");
        }
        window.inFlight++;
        windows.put(userId, window);
        return new AiRequestQuotaPermit(() -> complete(userId, window, true),
                () -> complete(userId, window, false));
    }

    private Window currentWindow(Long userId, Instant now) {
        Window window = windows.getIfPresent(userId);
        if (window == null || !now.isBefore(window.startedAt.plus(DAY))) {
            window = new Window(now);
            windows.put(userId, window);
        }
        return window;
    }

    private synchronized void complete(Long userId, Window reservedWindow, boolean succeeded) {
        reservedWindow.inFlight = Math.max(0, reservedWindow.inFlight - 1);
        if (succeeded) reservedWindow.count++;
        if (windows.getIfPresent(userId) == reservedWindow) windows.put(userId, reservedWindow);
    }

    private static final class Window {
        private final Instant startedAt;
        private int count;
        private int inFlight;

        private Window(Instant startedAt) { this.startedAt = startedAt; }
    }
}
