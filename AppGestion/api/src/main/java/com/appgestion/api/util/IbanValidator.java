package com.appgestion.api.util;

import java.math.BigInteger;

/**
 * Validación de IBAN (ISO 13616, comprobación mod 97).
 */
public final class IbanValidator {

    private IbanValidator() {}

    public static boolean isValid(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String iban = raw.replaceAll("\\s+", "").toUpperCase();
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
}
