package appgestion.util;

/**
 * Utilidades para cadenas de texto.
 */
public final class StringUtils {

    private StringUtils() {}

    /**
     * Devuelve la cadena vacía si el argumento es null.
     */
    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
