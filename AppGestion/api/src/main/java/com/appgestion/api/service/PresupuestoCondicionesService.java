package com.appgestion.api.service;

import com.appgestion.api.domain.presupuesto.PresupuestoCondicionCatalogo;
import com.appgestion.api.dto.response.PresupuestoCondicionDisponibleResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Resuelve condiciones predefinidas y serialización JSON para persistencia.
 */
@Service
public class PresupuestoCondicionesService {

    private final ObjectMapper objectMapper;

    public PresupuestoCondicionesService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<PresupuestoCondicionDisponibleResponse> listarDisponibles() {
        return PresupuestoCondicionCatalogo.todasOrdenadas().stream()
                .map(d -> new PresupuestoCondicionDisponibleResponse(
                        d.clave(), d.textoPdf(), d.activaPorDefecto()))
                .toList();
    }

    public List<String> normalizarClaves(List<String> claves) {
        return PresupuestoCondicionCatalogo.normalizarClaves(claves);
    }

    /** Textos completos en orden de catálogo, para PDF. */
    public List<String> textosPdfEnOrden(List<String> clavesNormalizadas) {
        List<String> textos = new ArrayList<>();
        for (String k : clavesNormalizadas) {
            var def = PresupuestoCondicionCatalogo.porClave(k);
            if (def != null) {
                textos.add(def.textoPdf());
            }
        }
        return textos;
    }

    public String aJson(List<String> claves) {
        if (claves == null || claves.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalizarClaves(claves));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar condiciones", e);
        }
    }

    public List<String> desdeJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> raw = objectMapper.readValue(json, new TypeReference<>() {});
            return PresupuestoCondicionCatalogo.normalizarClaves(raw);
        } catch (Exception e) {
            return List.of();
        }
    }
}
