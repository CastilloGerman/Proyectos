package com.appgestion.api.scheduler;

import com.appgestion.api.service.UsuarioSesionCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Elimina filas antiguas de {@code usuario_sesion} cuya fecha de expiración del token ya pasó hace más de
 * {@code app.sessions.cleanup.retention-days} días, para que la tabla no crezca sin límite.
 */
@Component
public class UsuarioSesionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(UsuarioSesionCleanupJob.class);

    private final UsuarioSesionCleanupService usuarioSesionCleanupService;

    @Value("${app.sessions.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.sessions.cleanup.retention-days:30}")
    private int retentionDays;

    public UsuarioSesionCleanupJob(UsuarioSesionCleanupService usuarioSesionCleanupService) {
        this.usuarioSesionCleanupService = usuarioSesionCleanupService;
    }

    /** Diario a las 03:00 (misma zona que el servidor). */
    @Scheduled(cron = "0 0 3 * * ?")
    public void purgeExpiredSessions() {
        if (!cleanupEnabled) {
            return;
        }
        int days = Math.max(1, retentionDays);
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        int deleted = usuarioSesionCleanupService.deleteExpiredBefore(cutoff);
        if (deleted > 0) {
            log.info("Limpieza usuario_sesion: eliminadas {} filas con expiresAt anterior a {} (retención {} días)",
                    deleted, cutoff, days);
        }
    }
}
