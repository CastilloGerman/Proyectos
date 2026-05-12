package com.appgestion.api.util;

/**
 * Texto visible en correos: marca y saludos unificados.
 */
public final class EmailCopy {

    public static final String PRODUCT_NAME = "Noemi";

    /**
     * Nombre de producto en texto promocional (invitaciones, compartir enlace).
     */
    public static final String PRODUCT_MARKETING_NAME = "Noemi Web";

    /**
     * Mensaje igual al compartir enlace (WhatsApp, Web Share) en Angular. También puede usarse como resumen verbal de la marca.
     */
    public static final String INVITE_SHARE_TAGLINE =
            "Prueba Noemi Web — gestión para autónomos con presupuestos y facturación";

    private EmailCopy() {
    }

    /**
     * Apertura del correo de referido (no usa el nombre de la empresa que envía).
     */
    public static String inviteReferralTeamOpeningParagraphHtml() {
        return "<p>Le contactamos del equipo de " + htmlEscape(PRODUCT_NAME) + ".</p>";
    }

    /**
     * Qué es el producto; misma línea conceptual que describe la web antes del enlace de alta.
     */
    public static final String INVITE_REFERRAL_PRODUCT_LINE =
            "Noemi es la web de gestión para autónomos con presupuestos y facturación";

    public static String inviteReferralProductParagraphHtml() {
        return "<p>" + htmlEscape(INVITE_REFERRAL_PRODUCT_LINE) + ".</p>";
    }

    public static String inviteReferralSomeoneSentLinkParagraphHtml() {
        return "<p>Alguien te ha enviado un enlace para crear tu propia cuenta en " + htmlEscape(PRODUCT_NAME) + ".</p>"
                + "<p>Te lleva a la pantalla de acceso de la aplicación; puedes <strong>darte de alta indicando el correo electrónico que prefieras</strong>; "
                + "no tiene que coincidir con la dirección donde recibiste este mensaje.</p>";
    }

    public static String inviteReferralTrialOneMonthParagraphHtml() {
        return "<p>Tendrás un periodo de prueba gratis de 1 mes para poder conocernos; después, para "
                + "seguir trabajando, creando y editando necesitarás una suscripción activa.</p>";
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
