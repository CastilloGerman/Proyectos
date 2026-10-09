package com.appgestion.api.dto.response;

import java.time.Instant;

public record PresupuestoEnlaceEstadoResponse(boolean activo, Instant expiraAt, Instant primeraVistaAt,
                                              Instant ultimaVistaAt, long numVistas, int enlacesActivos) {}
