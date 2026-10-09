package com.appgestion.api.scheduler;

import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.PresupuestoSeguimientoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PresupuestoSeguimientoJob {
    private static final Logger log = LoggerFactory.getLogger(PresupuestoSeguimientoJob.class);

    private final UsuarioRepository usuarioRepository;
    private final PresupuestoSeguimientoService seguimientoService;

    public PresupuestoSeguimientoJob(UsuarioRepository usuarioRepository,
                                    PresupuestoSeguimientoService seguimientoService) {
        this.usuarioRepository = usuarioRepository;
        this.seguimientoService = seguimientoService;
    }

    @Scheduled(cron = "${app.presupuesto-seguimiento.cron:0 0 9 * * ?}", zone = "Europe/Madrid")
    public void ejecutarDiario() {
        ejecutar(null);
    }

    public int ejecutarManualLocal() {
        return ejecutar(seguimientoService.localDaysOverride());
    }

    private int ejecutar(Integer daysOverride) {
        int total = 0;
        int failedOwners = 0;
        Long lastOwnerId = 0L;
        Page<Long> batch;
        do {
            batch = usuarioRepository.findFollowupOwnerIdsAfter(
                    lastOwnerId, PageRequest.of(0, 100));
            for (Long ownerId : batch.getContent()) {
                lastOwnerId = ownerId;
                try {
                    total += seguimientoService.processOwner(ownerId, daysOverride);
                } catch (RuntimeException failure) {
                    failedOwners++;
                    log.error("Fallo de seguimiento aislado: usuarioId={} avisos=0 errores=1", ownerId);
                }
            }
        }
        while (batch.hasNext());
        log.info("Seguimiento diario completado: avisos={} propietariosConError={}", total, failedOwners);
        return total;
    }
}
