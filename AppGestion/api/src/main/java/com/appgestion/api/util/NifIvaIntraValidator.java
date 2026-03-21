package com.appgestion.api.util;

import java.util.regex.Pattern;

/** Formato simplificado NIF-IVA intracomunitario (UE), p. ej. ESB12345678. */
public final class NifIvaIntraValidator {

    private static final Pattern PATTERN = Pattern.compile("^[A-Z]{2}[A-Z0-9]{2,12}$");

    private NifIvaIntraValidator() {}

    public static boolean isValid(String normalizedUpperNoSpaces) {
        if (normalizedUpperNoSpaces == null || normalizedUpperNoSpaces.isEmpty()) {
            return true;
        }
        return PATTERN.matcher(normalizedUpperNoSpaces).matches();
    }
}
