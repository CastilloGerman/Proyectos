package com.appgestion.api.service;

import com.appgestion.api.security.TotpService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SEC-12: verificación de códigos TOTP con protección frente a fuerza bruta y reutilización.
 * <ul>
 *   <li><b>Bloqueo por usuario</b>: tras {@link #MAX_FAILED_ATTEMPTS} códigos erróneos seguidos, se rechaza
 *       cualquier intento (429) durante {@link #LOCK_DURATION}. El estado vive en memoria (Caffeine acotada).</li>
 *   <li><b>Anti-replay</b>: un código aceptado no vale una segunda vez dentro de {@link #REPLAY_WINDOW}
 *       (ventana TOTP de 30 s con tolerancia de ±1 paso).</li>
 * </ul>
 */
@Service
public class TotpVerificationService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    public static final Duration REPLAY_WINDOW = Duration.ofSeconds(100);
    private static final long MAX_TRACKED_USERS = 50_000;

    private final TotpService totpService;

    /** Fallos consecutivos por usuario; la entrada caduca {@link #LOCK_DURATION} después del último fallo. */
    private final Cache<Long, AtomicInteger> failedAttempts = Caffeine.newBuilder()
            .expireAfterWrite(LOCK_DURATION)
            .maximumSize(MAX_TRACKED_USERS)
            .build();

    /** Último código aceptado por usuario (clave = usuario, valor = código). */
    private final Cache<Long, String> lastAcceptedCode = Caffeine.newBuilder()
            .expireAfterWrite(REPLAY_WINDOW)
            .maximumSize(MAX_TRACKED_USERS)
            .build();

    public TotpVerificationService(TotpService totpService) {
        this.totpService = totpService;
    }

    /**
     * @return {@code true} si el código es válido y no se había usado ya.
     * @throws ResponseStatusException 429 si el usuario está bloqueado por demasiados fallos.
     */
    public boolean verify(Long usuarioId, String secretBase32, String code) {
        if (usuarioId == null) {
            return false;
        }
        AtomicInteger fails = failedAttempts.getIfPresent(usuarioId);
        if (fails != null && fails.get() >= MAX_FAILED_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiados intentos fallidos con el código de verificación. Inténtalo de nuevo en unos minutos.");
        }

        String normalized = code == null ? "" : code.trim().replaceAll("\\s+", "");
        if (!totpService.verify(secretBase32, normalized)) {
            registerFailure(usuarioId);
            return false;
        }
        if (normalized.equals(lastAcceptedCode.getIfPresent(usuarioId))) {
            // Código válido pero ya consumido: replay. No cuenta como fallo, pero tampoco se acepta.
            return false;
        }
        lastAcceptedCode.put(usuarioId, normalized);
        failedAttempts.invalidate(usuarioId);
        return true;
    }

    private void registerFailure(Long usuarioId) {
        // compute() reinicia el contador de expiración (expireAfterWrite) en cada fallo.
        failedAttempts.asMap().compute(usuarioId,
                (id, current) -> new AtomicInteger(current != null ? current.get() + 1 : 1));
    }
}
