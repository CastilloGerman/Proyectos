package com.appgestion.api.repository.projection;

import java.time.Instant;

public interface PresupuestoEnlaceResumen {
    Instant getPrimeraVistaAt();
    Instant getUltimaVistaAt();
    Long getNumVistas();
}
