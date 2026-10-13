package com.appgestion.api.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SEC-14: política del remitente ({@code From}) configurable por el usuario en el modo de envío "system".
 * <p>
 * El usuario solo puede personalizar el <b>nombre visible</b>; la dirección debe pertenecer al dominio del remitente
 * del sistema (el único verificado con SPF/DKIM). Así no puede suplantar a terceros ni dañar la reputación del
 * dominio con direcciones ajenas. El nombre visible no puede contener {@code @}, {@code <}, {@code >}, comillas ni
 * saltos de línea (evita "Banco <x@banco.com>" camuflado como nombre e inyección de cabeceras).
 */
public final class MailFromPolicy {

    public static final int MAX_DISPLAY_NAME = 80;

    private static final Pattern ADDRESS = Pattern.compile("^[A-Za-z0-9._%+-]{1,64}@([A-Za-z0-9.-]{1,253})$");
    private static final Pattern NAME_AND_ADDRESS = Pattern.compile("^([^<>\\r\\n]*?)\\s*<([^<>\\s]+)>$");
    private static final Pattern FORBIDDEN_IN_NAME = Pattern.compile("[@<>\"\\r\\n\\u0000-\\u001f]");

    private MailFromPolicy() {
    }

    /**
     * @return el remitente normalizado ({@code Nombre <dir@dominio>} o {@code dir@dominio}).
     * @throws IllegalArgumentException si el formato no es válido o el dominio no coincide con el del sistema
     */
    public static String validate(String candidate, String platformFrom) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalArgumentException("El remitente no puede estar vacío");
        }
        String trimmed = candidate.trim();
        String name = null;
        String address = trimmed;
        Matcher m = NAME_AND_ADDRESS.matcher(trimmed);
        if (m.matches()) {
            name = m.group(1).trim();
            address = m.group(2).trim();
        }
        Matcher a = ADDRESS.matcher(address);
        if (!a.matches()) {
            throw new IllegalArgumentException("El remitente no tiene un formato de email válido");
        }
        if (name != null && !name.isEmpty()) {
            if (name.length() > MAX_DISPLAY_NAME || FORBIDDEN_IN_NAME.matcher(name).find()) {
                throw new IllegalArgumentException("El nombre del remitente contiene caracteres no permitidos o es demasiado largo");
            }
        }
        String platformDomain = domainOf(platformFrom);
        String domain = a.group(1).toLowerCase(Locale.ROOT);
        if (platformDomain == null || !(domain.equals(platformDomain) || domain.endsWith("." + platformDomain))) {
            throw new IllegalArgumentException(
                    "El remitente debe usar el dominio de envío de la plataforma" + (platformDomain != null ? " (" + platformDomain + ")" : ""));
        }
        String normalizedAddress = address.substring(0, address.indexOf('@')) + "@" + domain;
        return (name == null || name.isEmpty()) ? normalizedAddress : name + " <" + normalizedAddress + ">";
    }

    /** @return {@code true} si el remitente cumple la política (sin lanzar). */
    public static boolean isAllowed(String candidate, String platformFrom) {
        try {
            validate(candidate, platformFrom);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Dominio (en minúsculas) del remitente del sistema, o {@code null} si no se puede determinar. */
    static String domainOf(String from) {
        if (from == null || from.isBlank()) {
            return null;
        }
        String s = from.trim();
        Matcher m = NAME_AND_ADDRESS.matcher(s);
        String address = m.matches() ? m.group(2) : s;
        Matcher a = ADDRESS.matcher(address);
        return a.matches() ? a.group(1).toLowerCase(Locale.ROOT) : null;
    }
}
