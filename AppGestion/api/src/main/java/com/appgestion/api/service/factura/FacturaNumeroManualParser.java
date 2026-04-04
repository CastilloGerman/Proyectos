package com.appgestion.api.service.factura;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parseo y validación de formato del número manual FAC-AAAA-NNNN.
 */
public final class FacturaNumeroManualParser {

    private static final Pattern FAC_NUMERO_PATTERN = Pattern.compile("^FAC-(\\d{4})-(\\d+)$", Pattern.CASE_INSENSITIVE);

    private FacturaNumeroManualParser() {
    }

    public record ParsedFac(int anio, int secuencial) {}

    public static ParsedFac parseFacManual(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher m = FAC_NUMERO_PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            return null;
        }
        return new ParsedFac(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
    }
}
