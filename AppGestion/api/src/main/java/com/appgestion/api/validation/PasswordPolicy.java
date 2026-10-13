package com.appgestion.api.validation;

import java.util.Locale;
import java.util.Set;

/**
 * SEC-13: política de contraseñas nuevas.
 * <ul>
 *   <li>Entre {@link #MIN_LENGTH} y {@link #MAX_LENGTH} caracteres (bcrypt trunca a 72 bytes; el máximo evita
 *       hashear entradas gigantes).</li>
 *   <li>Sin reglas de composición (mayúsculas, símbolos...): se priorizan longitud y no usar claves conocidas.</li>
 *   <li>Rechaza un carácter repetido, secuencias triviales y una lista de contraseñas muy comunes.</li>
 * </ul>
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> COMMON = Set.of(
            "password1234", "password12345", "password123456", "contrasena123", "contrasena1234",
            "contrasena12345", "123456789012", "1234567890123", "12345678901234", "qwertyuiop12",
            "qwertyuiopas", "qwertyuiop123", "asdfghjkl123", "asdfghjkl1234", "abcdefghijkl",
            "abcdefgh1234", "iloveyou1234", "letmein12345", "welcome12345", "administrator",
            "admin1234567", "changeme1234", "passw0rd1234", "p@ssw0rd1234", "password!234",
            "1q2w3e4r5t6y", "1qaz2wsx3edc", "zaq12wsxcde3", "qazwsxedcrfv", "000000000000",
            "111111111111", "123123123123", "123456123456", "abc123abc123", "football1234",
            "mypassword123", "mipassword123", "micontrasena1", "micontrasena12", "noemi1234567");

    private static final String SEQUENCES = "0123456789012345678901234567890"
            + "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
            + "qwertyuiopasdfghjklzxcvbnm";

    private PasswordPolicy() {
    }

    /** @return {@code null} si es aceptable o el motivo (legible) por el que se rechaza. */
    public static String violation(String password) {
        if (password == null || password.isBlank()) {
            return "La contraseña es obligatoria";
        }
        int len = password.codePointCount(0, password.length());
        if (len < MIN_LENGTH || len > MAX_LENGTH) {
            return "La contraseña debe tener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " caracteres";
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (lower.chars().distinct().count() <= 2) {
            return "La contraseña es demasiado simple";
        }
        if (COMMON.contains(lower) || SEQUENCES.contains(lower)) {
            return "La contraseña es demasiado común. Elige otra más difícil de adivinar";
        }
        return null;
    }
}
