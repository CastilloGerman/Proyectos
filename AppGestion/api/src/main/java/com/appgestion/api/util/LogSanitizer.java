package com.appgestion.api.util;

/**
 * SEC-23: utilidades para no escribir datos personales ni secretos en los logs.
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    /**
     * Enmascara un email conservando lo justo para correlacionar incidencias: {@code juan.perez@dominio.com} pasa a
     * {@code j***@dominio.com}. Valores nulos, vacíos o sin {@code @} devuelven {@code "***"}.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }
        String e = email.trim();
        int at = e.indexOf('@');
        if (at <= 0 || at == e.length() - 1) {
            return "***";
        }
        // El dominio se conserva (útil para diagnosticar proveedores); el usuario local se reduce a su inicial.
        String domain = e.substring(at + 1).replaceAll("[\\r\\n]", "");
        return e.charAt(0) + "***@" + domain;
    }

    /** Elimina saltos de línea de un valor controlado por el cliente antes de registrarlo (log forging). */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String v = value.replaceAll("[\\r\\n\\t]", "_");
        return v.length() > 200 ? v.substring(0, 200) + "…" : v;
    }
}
