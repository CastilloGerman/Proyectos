package com.appgestion.api.dto.response;

import com.appgestion.api.domain.enums.FiscalPlazoEstado;

import java.time.LocalDate;

/**
 * Plazo de presentación del IVA (Modelo 303) más próximo respecto a la fecha actual.
 * Los plazos son reglas fijas orientativas; la AEAT puede publicar calendarios o prórrogas.
 */
public record FiscalPlazoActualResponse(
        String trimestre,
        LocalDate fechaLimite,
        long diasRestantes,
        FiscalPlazoEstado estado,
        String mensaje,
        boolean plazoAnteriorVencido,
        String mensajeAdvertencia
) {
}
