package com.appgestion.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Genera un PDF de muestra con OpenPDF (misma pipeline que producción). {@code notasPie} null = usar texto
 * guardado en empresa; cadena vacía = sin notas al pie en el PDF.
 */
public record PlantillasPdfPreviewRequest(
        @NotNull PlantillaPdfTipo tipo,
        @Size(max = 1000) String notasPie,
        PlantillaPdfEscenario escenario
) {}
