package com.appgestion.api.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validación de IBAN (ISO 13616, comprobación mod 97).
 */
public final class IbanValidator {

    private IbanValidator() {}

    /** Espacios, NBSP, puntos y guiones (mismo criterio que el cliente). */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.replaceAll("[\\s\\u00A0.\\-]+", "").toUpperCase();
    }

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String iban = normalize(raw);
        if (iban.length() < 15 || iban.length() > 34) {
            return false;
        }
        if (!iban.matches("[A-Z0-9]+")) {
            return false;
        }
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (int i = 0; i < rearranged.length(); i++) {
            char c = rearranged.charAt(i);
            if (c >= '0' && c <= '9') {
                numeric.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                numeric.append(c - 'A' + 10);
            } else {
                return false;
            }
        }
        try {
            return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * IBAN con un espacio cada 4 caracteres (sin espacio final), para mostrar en PDF.
     */
    public static String formatForPdfDisplay(String raw) {
        String n = normalize(raw);
        if (n.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length(); i += 4) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(n, i, Math.min(i + 4, n.length()));
        }
        return sb.toString();
    }

    /**
     * Parte el IBAN ya formateado en líneas de como mucho {@code maxLineLength} caracteres (saltos visuales en PDF).
     */
    public static List<String> wrapFormattedIbanForPdf(String formatted, int maxLineLength) {
        if (formatted == null || formatted.isEmpty()) {
            return Collections.emptyList();
        }
        int max = Math.max(8, maxLineLength);
        List<String> lines = new ArrayList<>();
        for (int start = 0; start < formatted.length(); start += max) {
            int end = Math.min(start + max, formatted.length());
            lines.add(formatted.substring(start, end));
        }
        return lines;
    }
}
