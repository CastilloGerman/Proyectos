package com.appgestion.api.repository;

import java.time.Instant;

public interface PresupuestoSeguimientoAvisoRepositoryCustom {
    int insertIfAbsent(Long presupuestoId, String tipo, int numeroAviso, Instant creadoAt);
}
