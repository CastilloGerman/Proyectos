package com.appgestion.api.dto.response;

import java.time.Instant;

public record PresupuestoEnlaceCreadoResponse(String url, Instant expiraAt) {}
