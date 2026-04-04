package com.appgestion.api.constant;

/**
 * Valores de {@code presupuestos.estado} usados en reglas de negocio (coinciden con BD y UI).
 */
public final class PresupuestoEstado {

    private PresupuestoEstado() {
    }

    public static final String PENDIENTE = "Pendiente";
    public static final String ACEPTADO = "Aceptado";
    public static final String RECHAZADO = "Rechazado";
    /** Con tilde (UI) y sin tilde (variante legada). */
    public static final String EN_EJECUCION = "En ejecución";
    public static final String EN_EJECUCION_SIN_TILDE = "En ejecucion";
}
