package com.appgestion.api.dto.request;

/**
 * Datos sintéticos para la vista previa (líneas de ejemplo). LONG_FOOTER usa las mismas líneas que DEFAULT;
 * el pie largo lo aporta el texto del editor en el cliente.
 */
public enum PlantillaPdfEscenario {
    DEFAULT,
    /** Líneas con IVA aplicado y exento (columna IVA 21% / 0%). */
    MIXED_IVA,
    /** Descripciones muy largas para probar ajuste de texto en tabla. */
    LONG_LINES,
    LONG_FOOTER
}
