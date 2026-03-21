package com.appgestion.api.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Actualización parcial de textos que se imprimen al pie de PDFs de presupuesto y factura.
 */
public record PlantillasPdfPatchRequest(
        @Size(max = 1000) String notasPiePresupuesto,
        @Size(max = 1000) String notasPieFactura
) {}
