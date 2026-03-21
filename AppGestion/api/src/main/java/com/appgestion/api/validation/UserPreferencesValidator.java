package com.appgestion.api.validation;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;

/**
 * Listas blancas para preferencias de usuario (evita valores arbitrarios / inyección en logs).
 */
public final class UserPreferencesValidator {

    private static final Set<String> LOCALES = Set.of("es", "en");
    private static final Set<String> CURRENCIES = Set.of("EUR", "USD", "GBP", "MXN", "COP", "ARS", "CLP");

    private UserPreferencesValidator() {}

    public static String normalizeAndValidateLocale(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Idioma no válido");
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (!LOCALES.contains(v)) {
            throw new IllegalArgumentException("Idioma no soportado. Usa es o en.");
        }
        return v;
    }

    public static String normalizeAndValidateTimeZone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Zona horaria no válida");
        }
        String v = raw.trim();
        if (v.length() > 64) {
            throw new IllegalArgumentException("Zona horaria demasiado larga");
        }
        try {
            ZoneId.of(v);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Zona horaria no reconocida");
        }
        return v;
    }

    public static String normalizeAndValidateCurrency(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Moneda no válida");
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (v.length() != 3 || !CURRENCIES.contains(v)) {
            throw new IllegalArgumentException("Moneda no soportada");
        }
        return v;
    }
}
