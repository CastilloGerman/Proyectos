package com.appgestion.api.domain.presupuesto;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catálogo único de condiciones para presupuestos (texto legal orientativo).
 * La fuente de verdad está aquí; el front solo muestra claves y textos devueltos por la API.
 */
public final class PresupuestoCondicionCatalogo {

    private static final Map<String, Def> POR_CLAVE;

    static {
        Map<String, Def> m = new LinkedHashMap<>();
        agregar(m, "validez_30_dias",
                "Validez del presupuesto: 30 días desde la fecha de emisión.",
                false);
        agregar(m, "pago_50_50",
                "Forma de pago: 50% al aceptar el presupuesto, 50% a la finalización de los trabajos.",
                false);
        agregar(m, "materiales_no_incluidos",
                "Los materiales no están incluidos salvo indicación expresa.",
                false);
        agregar(m, "variacion_precio_materiales",
                "El precio puede estar sujeto a variaciones en el coste de los materiales.",
                false);
        agregar(m, "mano_obra_desplazamiento",
                "Los trabajos incluyen mano de obra y desplazamiento.",
                false);
        agregar(m, "garantia_1_anio",
                "Garantía de mano de obra: 1 año desde la finalización.",
                false);
        POR_CLAVE = Collections.unmodifiableMap(m);
    }

    private static void agregar(Map<String, Def> map, String clave, String textoPdf, boolean activaPorDefecto) {
        map.put(clave, new Def(clave, textoPdf, activaPorDefecto));
    }

    private PresupuestoCondicionCatalogo() {}

    public static List<Def> todasOrdenadas() {
        return List.copyOf(POR_CLAVE.values());
    }

    public static Def porClave(String clave) {
        return clave != null ? POR_CLAVE.get(clave) : null;
    }

    /** Claves recibidas filtradas y ordenadas como en el catálogo (orden estable del mapa). */
    public static List<String> normalizarClaves(List<String> claves) {
        if (claves == null || claves.isEmpty()) {
            return List.of();
        }
        var pedidas = new HashSet<>(claves);
        return POR_CLAVE.keySet().stream()
                .filter(pedidas::contains)
                .collect(Collectors.toList());
    }

    public record Def(String clave, String textoPdf, boolean activaPorDefecto) {}
}
