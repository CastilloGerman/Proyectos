package com.appgestion.api.unit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresupuestoIaCasosEvaluationFixtureTest {

    @Test
    void containsFifteenCasesWithAtLeastSixDistractorsAndNoMatchCases() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream stream = getClass().getResourceAsStream("/ia-presupuesto-casos.json")) {
            JsonNode cases = mapper.readTree(stream);
            long distractors = count(cases, "distractor");
            long noMatch = count(cases, "sin_material");

            assertEquals(15, cases.size());
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
}
