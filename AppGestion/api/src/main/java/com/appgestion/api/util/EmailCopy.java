package com.appgestion.api.util;

/**
 * Texto visible en correos: marca y saludos unificados.
 */
public final class EmailCopy {

    public static final String PRODUCT_NAME = "Noemi";

    private EmailCopy() {
    }

    public static String htmlEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Inicio de mensaje para correos al cliente (externo): "{cliente}, le contactamos de {empresa}."
     */
    public static String prefijoClienteEmpresa(String nombreCliente, String nombreEmpresa) {
        String nc = (nombreCliente != null && !nombreCliente.isBlank()) ? nombreCliente.trim() : "—";
        String ne = nombreEmpresaOrDefault(nombreEmpresa);
        return "<p>" + htmlEscape(nc) + ", le contactamos de " + htmlEscape(ne) + ".</p>";
    }

    /**
     * Inicio para correos al usuario autónomo u otro destinatario que no es "cliente" en facturación:
     * "{nombre}, le contactamos de {empresa}."
     */
    public static String prefijoDestinatarioEmpresa(String nombreDestinatario, String nombreEmpresa) {
        String nd = (nombreDestinatario != null && !nombreDestinatario.isBlank()) ? nombreDestinatario.trim() : "Hola";
        String ne = nombreEmpresaOrDefault(nombreEmpresa);
        return "<p>" + htmlEscape(nd) + ", le contactamos de " + htmlEscape(ne) + ".</p>";
    }

    /** Solo la frase "Le contactamos de {empresa}." (p. ej. invitación genérica). */
    public static String prefijoSoloEmpresa(String nombreEmpresa) {
        return "<p>Le contactamos de " + htmlEscape(nombreEmpresaOrDefault(nombreEmpresa)) + ".</p>";
    }

    private static String nombreEmpresaOrDefault(String nombreEmpresa) {
        return (nombreEmpresa != null && !nombreEmpresa.isBlank()) ? nombreEmpresa.trim() : PRODUCT_NAME;
    }
}
