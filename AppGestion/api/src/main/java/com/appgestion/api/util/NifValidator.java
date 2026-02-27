package com.appgestion.api.util;

/**
 * Validador de NIF español (DNI, NIE, CIF) conforme a normativa española.
 * DNI: 8 dígitos + letra (módulo 23).
 * NIE: X/Y/Z + 7 dígitos + letra (X=0, Y=1, Z=2, mismo algoritmo módulo 23).
 * CIF: Letra tipo + 7 dígitos + dígito/letra de control (algoritmo AEAT suma ponderada).
 */
public final class NifValidator {

    private static final String DNI_LETRAS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final String CIF_LETRAS_CONTROL = "JABCDEFGHI";
    private static final int[] CIF_CALCULO = {0, 2, 4, 6, 8, 1, 3, 5, 7, 9};
    private NifValidator() {
    }

    /**
     * Valida si el NIF es correcto (DNI, NIE o CIF).
     *
     * @param nif NIF a validar (puede contener espacios/guiones, se normaliza)
     * @return true si es válido
     */
    public static boolean esValido(String nif) {
        if (nif == null || nif.isBlank()) {
            return false;
        }
        String n = normalizar(nif);
        if (n.length() != 9) {
            return false;
        }
        if (esCif(n)) {
            return validarCif(n);
        }
        if (esNie(n)) {
            return validarNie(n);
        }
        if (esDni(n)) {
            return validarDni(n);
        }
        return false;
    }

    /**
     * Detecta el tipo de NIF.
     */
    public static TipoNif detectarTipo(String nif) {
        if (nif == null || nif.isBlank()) {
            return TipoNif.DESCONOCIDO;
        }
        String n = normalizar(nif);
        if (n.length() != 9) {
            return TipoNif.DESCONOCIDO;
        }
        if (esCif(n)) {
            return TipoNif.CIF;
        }
        if (esNie(n)) {
            return TipoNif.NIE;
        }
        if (esDni(n)) {
            return TipoNif.DNI;
        }
        return TipoNif.DESCONOCIDO;
    }

    private static String normalizar(String nif) {
        return nif.replaceAll("[\\s\\-]", "").toUpperCase();
    }

    private static boolean esDni(String n) {
        return n.matches("\\d{8}[A-Z]");
    }

    private static boolean esNie(String n) {
        return n.matches("[XYZ]\\d{7}[A-Z]");
    }

    private static boolean esCif(String n) {
        return n.matches("[A-HJ-NP-SUVW]\\d{7}[0-9A-J]");
    }

    private static boolean validarDni(String dni) {
        int num = Integer.parseInt(dni.substring(0, 8));
        char letra = dni.charAt(8);
        return DNI_LETRAS.charAt(num % 23) == letra;
    }

    private static boolean validarNie(String nie) {
        char primero = nie.charAt(0);
        int digitoInicial = switch (primero) {
            case 'X' -> 0;
            case 'Y' -> 1;
            case 'Z' -> 2;
            default -> -1;
        };
        int num = digitoInicial * 10_000_000 + Integer.parseInt(nie.substring(1, 8));
        char letra = nie.charAt(8);
        return DNI_LETRAS.charAt(num % 23) == letra;
    }

    private static boolean validarCif(String cif) {
        String sieteDigitos = cif.substring(1, 8);
        int a = 0;
        int b = 0;
        for (int i = 0; i < 7; i++) {
            int d = Character.getNumericValue(sieteDigitos.charAt(i));
            if (i % 2 == 0) {
                b += CIF_CALCULO[d];
            } else {
                a += d;
            }
        }
        int c = a + b;
        int d = (10 - (c % 10)) % 10;

        char control = cif.charAt(8);
        if (Character.isDigit(control)) {
            return Character.getNumericValue(control) == d;
        }
        return CIF_LETRAS_CONTROL.charAt(d) == control;
    }

    public enum TipoNif {
        DNI,
        NIE,
        CIF,
        DESCONOCIDO
    }
}
