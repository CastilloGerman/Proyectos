package com.appgestion.api.unit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresupuestoIaCasosEvaluationFixtureTest {

    @Test
    void containsTwentyCasesWithFivePriceRegressionsAndCatalogDistractors() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream stream = getClass().getResourceAsStream("/ia-presupuesto-casos.json")) {
            JsonNode cases = mapper.readTree(stream);
            long distractors = count(cases, "distractor");
            long noMatch = count(cases, "sin_material");

            assertEquals(20, cases.size());
            assertEquals(5, countPriceCases(cases));
            assertTrue(distractors >= 6);
            assertTrue(noMatch >= 6);
        }
    }

    private static long count(JsonNode cases, String type) {
        long count = 0;
        for (JsonNode testCase : cases) {
            if (type.equals(testCase.path("esperado").path("tipoCatalogo").asText())) count++;
        }
        return count;
    }

    private static long countPriceCases(JsonNode cases) {
        long count = 0;
        for (JsonNode testCase : cases) {
            if (testCase.path("esperado").path("totalEsperado").isNumber()) count++;
        }
        return count;
    }
}
