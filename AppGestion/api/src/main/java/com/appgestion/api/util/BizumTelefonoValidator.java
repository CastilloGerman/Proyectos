package com.appgestion.api.util;

import java.util.regex.Pattern;

/** Teléfono móvil español habitual para Bizum (9 dígitos, empieza en 6–9). */
public final class BizumTelefonoValidator {

    private static final Pattern NORMALIZADO = Pattern.compile("^\\d{9}$");
    private static final Pattern MOVIL_ES = Pattern.compile("^[6-9]\\d{8}$");

    private BizumTelefonoValidator() {}

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+34")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0034")) {
            digits = digits.substring(4);
        }
        digits = digits.replace("+", "");
        if (!NORMALIZADO.matcher(digits).matches()) {
            return false;
        }
        return MOVIL_ES.matcher(digits).matches();
    }
}
