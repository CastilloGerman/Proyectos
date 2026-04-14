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

/**
 * Plazos orientativos Modelo 303 (Bloque 5): fechas concretas del enunciado y umbrales de semáforo.
 */
class PlazosTrimestralesTest {

    private static final ZoneId Z = ZoneId.of("Europe/Madrid");

    private static FiscalPlazosService svc(Clock clock) {
        return new FiscalPlazosService(clock);
    }

    @Test
    void hoy15Enero_plazoActivoT4AnioAnterior_limite30Enero_dias15_amarillo() {
        LocalDate hoy = LocalDate.of(2026, 1, 15);
        FiscalPlazoActualResponse r = svc(fixedAt(hoy)).calcularParaFecha(hoy);
        assertEquals("T4 2025", r.trimestre());
        assertEquals(LocalDate.of(2026, 1, 30), r.fechaLimite());
        assertEquals(15, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.AMARILLO, r.estado());
    }

    @Test
    void hoy5Abril_plazoActivoT1_limite20Abril_dias15_amarillo() {
        LocalDate hoy = LocalDate.of(2026, 4, 5);
        FiscalPlazoActualResponse r = svc(fixedAt(hoy)).calcularParaFecha(hoy);
        assertEquals("T1 2026", r.trimestre());
        assertEquals(LocalDate.of(2026, 4, 20), r.fechaLimite());
        assertEquals(15, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.AMARILLO, r.estado());
    }

    @Test
    void hoy25Julio_plazoActivoT3_limite20Octubre_verde() {
        LocalDate hoy = LocalDate.of(2026, 7, 25);
        FiscalPlazoActualResponse r = svc(fixedAt(hoy)).calcularParaFecha(hoy);
        assertEquals("T3 2026", r.trimestre());
        assertEquals(LocalDate.of(2026, 10, 20), r.fechaLimite());
        assertEquals(87, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.VERDE, r.estado());
    }

    @Test
    void diasRestantes_menosDe10_rojo() {
        LocalDate hoy = LocalDate.of(2026, 4, 15);
        FiscalPlazoActualResponse r = svc(fixedAt(hoy)).calcularParaFecha(hoy);
        assertEquals(5, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.ROJO, r.estado());
    }

    @Test
    void diasRestantes_entre10y30_amarillo() {
        LocalDate hoy = LocalDate.of(2026, 4, 10);
        FiscalPlazoActualResponse r = svc(fixedAt(hoy)).calcularParaFecha(hoy);
        assertEquals(10, r.diasRestantes());
        assertEquals(FiscalPlazoEstado.AMARILLO, r.estado());
    }

    @Test
    void diasRestantes_masDe30_verde() {
        LocalDate hoy = LocalDate.of(2026, 3, 1);
        FiscalPlazoActualResponse r = svc(fixedAt(hoy)).calcularParaFecha(hoy);
        assertEquals(FiscalPlazoEstado.VERDE, r.estado());
    }

    private static Clock fixedAt(LocalDate day) {
        return Clock.fixed(Instant.from(day.atStartOfDay(Z)), Z);
    }
}
