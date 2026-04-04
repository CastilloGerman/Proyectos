package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Cuerpo para guardar qué condiciones del catálogo vienen marcadas por defecto en presupuestos nuevos. */
public record PresupuestoCondicionesPredeterminadasRequest(
        @NotNull(message = "Indica la propiedad claves (puede ser una lista vacía)")
        List<@NotBlank @Size(max = 80) String> claves
) {}
