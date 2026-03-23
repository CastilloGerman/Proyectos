package com.appgestion.api.scheduler;

import com.appgestion.api.service.AuditAccessCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Purga eventos de auditoría más antiguos que la retención configurada (inmutabilidad + coste de almacenamiento).
 */
@Component
public class AuditAccessCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AuditAccessCleanupJob.class);

    private final AuditAccessCleanupService auditAccessCleanupService;

    @Value("${app.audit.access.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.audit.access.retention-days:365}")
    private int retentionDays;

    public AuditAccessCleanupJob(AuditAccessCleanupService auditAccessCleanupService) {
        this.auditAccessCleanupService = auditAccessCleanupService;
    }

    /** Diario 03:30 (tras limpieza de sesiones). */
    @Scheduled(cron = "0 30 3 * * ?")
    public void purgeOldAuditEvents() {
        if (!cleanupEnabled) {
            return;
        }
        int days = Math.max(30, retentionDays);
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        int deleted = auditAccessCleanupService.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Limpieza audit_access_event: eliminados {} registros anteriores a {} (retención {} días)",
                    deleted, cutoff, days);
        }
    }
}
