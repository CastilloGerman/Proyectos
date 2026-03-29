package com.appgestion.api.service;

import com.appgestion.api.domain.enums.FiscalPlazoEstado;
import com.appgestion.api.dto.response.FiscalPlazoActualResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Plazos de presentación orientativos del Modelo 303 (IVA trimestral en España).
 * <p>
 * Regla habitual (sujeta a calendario oficial AEAT): T1–T3 con cierre el día 20 del mes indicado;
 * T4 con cierre el 30 de enero del año siguiente al periodo. No sustituye la normativa ni avisos
 * de la sede electrónica.
 */
@Service
public class FiscalPlazosService {

    private final Clock clock;

    public FiscalPlazosService(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Calcula el siguiente plazo de presentación a partir de la fecha actual (zona del reloj inyectado).
     * El contexto de usuario puede usarse en el futuro (plazos personalizados, recordatorios guardados).
     */
    public FiscalPlazoActualResponse calcularPlazoActual() {
        LocalDate today = LocalDate.now(clock);
        return calcularParaFecha(today);
    }

    FiscalPlazoActualResponse calcularParaFecha(LocalDate today) {
        List<Plazo> plazos = construirPlazosParaRango(today.getYear());
        plazos.sort(Comparator.comparing(Plazo::fecha));

        Plazo siguiente = plazos.stream()
                .filter(p -> !p.fecha().isBefore(today))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay plazo calculado; amplía el rango de años."));

        long diasRestantes = ChronoUnit.DAYS.between(today, siguiente.fecha());
        if (diasRestantes < 0) {
            diasRestantes = 0;
        }

        FiscalPlazoEstado estado = estadoDesdeDias(diasRestantes);
        String mensaje = construirMensajePrincipal(siguiente.etiqueta(), diasRestantes);

        Plazo anterior = plazos.stream()
                .filter(p -> p.fecha().isBefore(today))
                .max(Comparator.comparing(Plazo::fecha))
                .orElse(null);

        boolean entreDos = anterior != null
                && today.isAfter(anterior.fecha())
                && today.isBefore(siguiente.fecha());
        String advertencia = null;
        if (entreDos) {
            advertencia = "El plazo del " + Objects.requireNonNull(anterior).etiqueta()
                    + " ha vencido. Presenta cuanto antes para minimizar recargos.";
        }

        return new FiscalPlazoActualResponse(
                siguiente.etiqueta(),
                siguiente.fecha(),
                diasRestantes,
                estado,
                mensaje,
                entreDos,
                advertencia
        );
    }

    /**
     * Genera los cuatro plazos por año en un rango suficiente para cubrir búsquedas de siguiente/anterior.
     */
    private static List<Plazo> construirPlazosParaRango(int anioCentral) {
        List<Plazo> out = new ArrayList<>();
        for (int y = anioCentral - 1; y <= anioCentral + 2; y++) {
            out.add(new Plazo("T1 " + y, LocalDate.of(y, 4, 20)));
            out.add(new Plazo("T2 " + y, LocalDate.of(y, 7, 20)));
            out.add(new Plazo("T3 " + y, LocalDate.of(y, 10, 20)));
            out.add(new Plazo("T4 " + y, LocalDate.of(y + 1, 1, 30)));
        }
        return out;
    }

    private static FiscalPlazoEstado estadoDesdeDias(long dias) {
        if (dias > 30) {
            return FiscalPlazoEstado.VERDE;
        }
        if (dias >= 10) {
            return FiscalPlazoEstado.AMARILLO;
        }
        return FiscalPlazoEstado.ROJO;
    }

    private static String construirMensajePrincipal(String trimestre, long dias) {
        if (dias == 0) {
            return "Hoy finaliza el plazo para presentar el " + trimestre + " (Modelo 303).";
        }
        if (dias == 1) {
            return "Te queda 1 día para presentar el " + trimestre + " (Modelo 303).";
        }
        return "Tienes " + dias + " días para presentar el " + trimestre + " (Modelo 303).";
    }

    private record Plazo(String etiqueta, LocalDate fecha) {}
}
