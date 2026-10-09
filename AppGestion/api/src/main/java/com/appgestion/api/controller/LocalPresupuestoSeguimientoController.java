package com.appgestion.api.controller;

import com.appgestion.api.scheduler.PresupuestoSeguimientoJob;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dev/presupuesto-seguimiento")
@Profile("local")
public class LocalPresupuestoSeguimientoController {
    private final PresupuestoSeguimientoJob job;

    public LocalPresupuestoSeguimientoController(PresupuestoSeguimientoJob job) {
        this.job = job;
    }

    @PostMapping("/ejecutar")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Map<String, Object>> ejecutarAhora() {
        return ResponseEntity.ok(Map.of("avisosCreados", job.ejecutarManualLocal()));
    }
}
