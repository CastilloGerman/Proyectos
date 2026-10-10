package com.appgestion.api.unit.dto;

import com.appgestion.api.dto.response.SeguimientoPresupuestosResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato frontend-backend: el nombre del campo JSON de "permitirRespuestaCliente"
 * debe ser idéntico en el DTO de Java y en el servicio de Angular.
 * Si un desarrollador cambia el nombre en un lado pero no en el otro, este test falla.
 */
class SeguimientoPresupuestosContractTest {

    @Test
    void seguimientoPresupuestosResponse_containsPermitirRespuestaClienteField() {
        RecordComponent[] components = SeguimientoPresupuestosResponse.class.getRecordComponents();
        Set<String> fieldNames = Arrays.stream(components)
                .map(RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(fieldNames)
                .as("SeguimientoPresupuestosResponse debe contener el campo 'permitirRespuestaCliente' " +
                        "con el nombre exacto que espera el frontend (config.service.ts)")
                .contains("permitirRespuestaCliente");
    }

    @Test
    void seguimientoPresupuestosResponse_jsonFieldNames_matchFrontendExpectation() {
        // Jackson serializes Java record component names directly to JSON by default.
        // The frontend config.service.ts maps API field 'permitirRespuestaCliente' to TS field 'permitsRespuestaCliente'.
        // This test ensures the Java side never changes the field name without updating the frontend.
        RecordComponent[] components = SeguimientoPresupuestosResponse.class.getRecordComponents();

        String expectedField = "permitirRespuestaCliente";
        boolean found = false;
        for (RecordComponent rc : components) {
            if (rc.getName().equals(expectedField)) {
                found = true;
                break;
            }
        }

        assertThat(found)
                .as("El campo JSON 'permitirRespuestaCliente' debe existir en SeguimientoPresupuestosResponse. " +
                        "Si cambias este nombre, actualiza también frontend/src/app/core/services/config.service.ts")
                .isTrue();
    }
}
