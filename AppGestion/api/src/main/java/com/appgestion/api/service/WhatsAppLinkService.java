package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Factura;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Genera enlaces wa.me para recordatorios (el autónomo abre WhatsApp con texto pre-rellenado).
 * No envía mensajes por API; integración Twilio/Meta queda para una fase posterior.
 */
public final class WhatsAppLinkService {

    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");

    private WhatsAppLinkService() {}

    /**
     * Número en formato internacional sin + (ej. 34600111222) o vacío si no válido.
     */
    public static String normalizarTelefonoEspana(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return "";
        }
        String digits = NON_DIGITS.matcher(telefono.trim()).replaceAll("");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.startsWith("34") && digits.length() >= 11) {
            return digits;
        }
        if (digits.length() == 9) {
            return "34" + digits;
        }
        if (digits.length() >= 10) {
            return digits;
        }
        return "";
    }

    public static String enlaceRecordatorioFactura(Factura factura) {
        Cliente c = factura.getCliente();
        if (c == null) {
            return null;
        }
        String phone = normalizarTelefonoEspana(c.getTelefono());
        if (phone.isEmpty()) {
            return null;
        }
        String clienteNombre = c.getNombre() != null ? c.getNombre() : "";
        String numero = factura.getNumeroFactura() != null ? factura.getNumeroFactura() : "";
        String msg = "Hola" + (clienteNombre.isBlank() ? "" : " " + clienteNombre)
                + ", te escribo respecto a la factura " + numero
                + ". ¿Podemos confirmar el pago? Gracias.";
        String encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8);
        return "https://wa.me/" + phone + "?text=" + encoded;
    }
}
