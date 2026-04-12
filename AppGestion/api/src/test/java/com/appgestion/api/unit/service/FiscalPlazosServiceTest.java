package com.appgestion.api.unit.service;

import com.appgestion.api.domain.enums.FiscalPlazoEstado;
import com.appgestion.api.dto.response.FiscalPlazoActualResponse;
import com.appgestion.api.service.FiscalPlazosService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiscalPlazosServiceTest {

    private static final ZoneId Z = ZoneId.of("Europe/Madrid");

    private static FiscalPlazosService svc(Clock clock) {
        return new FiscalPlazosService(clock);
    }

    @Test
    void trimestreActivo_t1_muestraPlazo20Abril() {
        LocalDate hoy = LocalDate.of(2025, 4, 20);
        FiscalPlazoActualResponse r = svc(fixedAt(hoy)).calcularParaFecha(hoy);
        assertEquals("T1 2025", r.trimestre());
        assertEquals(LocalDate.of(2025, 4, 20), r.fechaLimite());
        assertEquals(0, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.ROJO, r.estado());
        assertFalse(r.plazoAnteriorVencido());
    }

    @Test
    void entrePlazos_muestraAdvertenciaPeriodoAnterior() {
        FiscalPlazoActualResponse r = svc(fixedAt(LocalDate.of(2025, 3, 15)))
                .calcularParaFecha(LocalDate.of(2025, 3, 15));
        assertEquals("T1 2025", r.trimestre());
        assertTrue(r.plazoAnteriorVencido());
        assertTrue(r.mensajeAdvertencia() != null && r.mensajeAdvertencia().contains("T4 2024"));
    }

    @Test
    void umbral_verde_masDe30Dias() {
        FiscalPlazoActualResponse r = svc(fixedAt(LocalDate.of(2025, 3, 1)))
                .calcularParaFecha(LocalDate.of(2025, 3, 1));
        assertEquals(FiscalPlazoEstado.VERDE, r.estado());
    }

    @Test
    void umbral_amarillo_entre10y30() {
        FiscalPlazoActualResponse r = svc(fixedAt(LocalDate.of(2025, 4, 5)))
                .calcularParaFecha(LocalDate.of(2025, 4, 5));
        assertEquals("T1 2025", r.trimestre());
        assertEquals(15, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.AMARILLO, r.estado());
    }

    @Test
    void umbral_rojo_menosDe10() {
        FiscalPlazoActualResponse r = svc(fixedAt(LocalDate.of(2025, 4, 15)))
                .calcularParaFecha(LocalDate.of(2025, 4, 15));
        assertEquals(5, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.ROJO, r.estado());
    }

    @Test
    void t4_plazo30EneroAnoSiguiente() {
        FiscalPlazoActualResponse r = svc(fixedAt(LocalDate.of(2025, 12, 1)))
                .calcularParaFecha(LocalDate.of(2025, 12, 1));
        assertEquals("T4 2025", r.trimestre());
        assertEquals(LocalDate.of(2026, 1, 30), r.fechaLimite());
    }

    @Test
    void despuesDePlazo_pasaAlSiguienteTrimestre() {
        FiscalPlazoActualResponse r = svc(fixedAt(LocalDate.of(2025, 4, 25)))
                .calcularParaFecha(LocalDate.of(2025, 4, 25));
        assertEquals("T2 2025", r.trimestre());
        assertEquals(LocalDate.of(2025, 7, 20), r.fechaLimite());
        assertTrue(r.plazoAnteriorVencido());
        assertTrue(r.mensajeAdvertencia() != null && r.mensajeAdvertencia().contains("T1 2025"));
    }

    @Test
    void diaDelPlazo_ceroDias_rojo() {
        FiscalPlazoActualResponse r = svc(fixedAt(LocalDate.of(2025, 7, 20)))
                .calcularParaFecha(LocalDate.of(2025, 7, 20));
        assertEquals(0, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.ROJO, r.estado());
        assertEquals("T2 2025", r.trimestre());
    }

    @Test
    void despuesDe30Enero_siguienteEsT1() {
        FiscalPlazoActualResponse r = svc(fixedAt(LocalDate.of(2026, 2, 5)))
                .calcularParaFecha(LocalDate.of(2026, 2, 5));
        assertEquals("T1 2026", r.trimestre());
        assertEquals(LocalDate.of(2026, 4, 20), r.fechaLimite());
    }

    private static Clock fixedAt(LocalDate day) {
        return Clock.fixed(Instant.from(day.atStartOfDay(Z)), Z);
    }
}
