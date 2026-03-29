package com.appgestion.api.dto.request;

import java.util.List;

/** Cuerpo para guardar qué condiciones del catálogo vienen marcadas por defecto en presupuestos nuevos. */
public record PresupuestoCondicionesPredeterminadasRequest(List<String> claves) {}
