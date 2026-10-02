package com.appgestion.api.dto.request;

import com.appgestion.api.domain.enums.CanalEnvio;
import jakarta.validation.constraints.NotNull;

public record MarcarPresupuestoEnviadoRequest(@NotNull CanalEnvio canal) {}
