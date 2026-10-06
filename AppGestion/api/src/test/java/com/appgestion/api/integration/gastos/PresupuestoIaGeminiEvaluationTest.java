package com.appgestion.api.integration.gastos;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Material;
import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.dto.request.PresupuestoIaRequest;
import com.appgestion.api.dto.response.PresupuestoIaBorradorResponse;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.PresupuestoIaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
@EnabledIfEnvironmentVariable(named = "APP_AI_EVAL_GEMINI", matches = "true")
class PresupuestoIaGeminiEvaluationTest {

    @Autowired private PresupuestoIaService presupuestoIaService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private GeminiProperties geminiProperties;

    @Test
    void evaluatesFifteenSpanishConstructionTextsAgainstRealGemini() throws Exception {
        Usuario user = createEvaluationUser();
        saveMaterials(user);
        JsonNode cases = objectMapper.readTree(getClass().getResourceAsStream("/ia-presupuesto-casos.json"));
        int passed = 0;
        int totalItems = 0;
        int correctItems = 0;
        int inventedPrices = 0;
        int inventedQuantities = 0;
        int mismatchedMaterialIds = 0;
        int correctNoMaterial = 0;
        int omittedParts = 0;
        System.out.printf(Locale.ROOT, "modelo=%s casos=%d%n", geminiProperties.getModel(), cases.size());
        for (int i = 0; i < cases.size(); i++) {
            JsonNode testCase = cases.get(i);
            JsonNode expected = testCase.path("esperado");
            PresupuestoIaBorradorResponse result = presupuestoIaService.generarBorrador(
                    new PresupuestoIaRequest(testCase.path("texto").asText(), null), user);
            List<String> descriptions = result.items().stream().map(item -> item.tareaManual().toLowerCase(Locale.ROOT)).toList();
            List<String> expectedConcepts = new ArrayList<>();
            expected.path("conceptos").forEach(value -> expectedConcepts.add(value.asText()));
            List<Double> allowedQuantities = new ArrayList<>();
            expected.path("cantidadesPermitidas").forEach(value -> allowedQuantities.add(value.asDouble()));
            long conceptsFound = expectedConcepts.stream()
                    .filter(concept -> descriptions.stream().anyMatch(description -> description.contains(concept)))
                    .count();
            Set<String> matchedExpectedConcepts = new HashSet<>();
            for (String concept : expectedConcepts) {
                if (descriptions.stream().anyMatch(description -> description.contains(concept))) {
                    matchedExpectedConcepts.add(concept);
                }
            }
            int omittedInCase = expectedConcepts.size() - matchedExpectedConcepts.size();
            omittedParts += omittedInCase;
            boolean countInRange = result.items().size() >= expected.path("minPartidas").asInt()
                    && result.items().size() <= expected.path("maxPartidas").asInt();
            boolean conceptsMostlyFound = expectedConcepts.isEmpty() || conceptsFound * 2 >= expectedConcepts.size();
            if (countInRange && conceptsMostlyFound) passed++;
            totalItems += result.items().size();
            for (var item : result.items()) {
                String description = item.tareaManual().toLowerCase(Locale.ROOT);
                String matchedConcept = expectedConcepts.stream()
                        .filter(description::contains)
                        .filter(matchedExpectedConcepts::add)
                        .findFirst().orElse(null);
                if (matchedConcept != null
                        && (item.cantidad() == null || allowedQuantities.stream()
                        .anyMatch(quantity -> Math.abs(quantity - item.cantidad()) < 0.0001))) {
                    correctItems++;
                }
                if (item.cantidad() != null && allowedQuantities.stream()
                        .noneMatch(quantity -> Math.abs(quantity - item.cantidad()) < 0.0001)) {
                    inventedQuantities++;
                }
                var matchedMaterial = item.materialId() == null ? null
                        : materialRepository.findByIdAndUsuarioId(item.materialId(), user.getId()).orElse(null);
                if (item.precioUnitario() != null && item.precioUnitario() > 0
                        && (item.materialId() == null || matchedMaterial == null
                        || matchedMaterial.getPrecioUnitario() == null
                        || Math.abs(item.precioUnitario() - matchedMaterial.getPrecioUnitario()) > 0.0001)) {
                    inventedPrices++;
                }
                String expectedMaterialName = expectedMaterialName(expected, description);
                if (item.materialId() == null && expectedMaterialName == null) {
                    correctNoMaterial++;
                }
                if (item.materialId() != null) {
                    if (expectedMaterialName == null || matchedMaterial == null
                            || !matchedMaterial.getNombre().equalsIgnoreCase(expectedMaterialName)) {
                        mismatchedMaterialIds++;
                    }
                }
            }
            System.out.printf(Locale.ROOT, "caso=%02d partidas=%d esperadas=%d..%d tipo_catalogo=%s " +
                            "aciertos_concepto=%d/%d omitidas=%d%n",
                    i + 1, result.items().size(), expected.path("minPartidas").asInt(),
                    expected.path("maxPartidas").asInt(), expected.path("tipoCatalogo").asText("coincidencia"),
                    conceptsFound, expectedConcepts.size(), omittedInCase);
            result.items().forEach(item -> System.out.printf(Locale.ROOT,
                    "  partida=%s materialId=%s faltaPrecio=%s cantidadDudosa=%s confianza=%s%n",
                    item.tareaManual(), item.materialId(), item.faltaPrecio(), item.cantidadDudosa(), item.confianza()));
        }
        double correctPercent = totalItems == 0 ? 0 : correctItems * 100.0 / totalItems;
        System.out.printf(Locale.ROOT,
                "resumen modelo=%s casos_ok=%d/%d precios_inventados=%d cantidades_inventadas=%d " +
                        "falsos_positivos_material=%d aciertos_sin_material=%d partidas_omitidas=%d " +
                        "partidas_correctas=%d/%d porcentaje_partidas_correctas=%.1f%%%n",
                geminiProperties.getModel(), passed, cases.size(), inventedPrices, inventedQuantities,
                mismatchedMaterialIds, correctNoMaterial, omittedParts, correctItems, totalItems, correctPercent);
    }

    private static String expectedMaterialName(JsonNode expected, String description) {
        for (JsonNode link : expected.path("materialesEsperados")) {
            if (description.contains(link.path("concepto").asText().toLowerCase(Locale.ROOT))) {
                return link.path("material").asText();
            }
        }
        return null;
    }

    private Usuario createEvaluationUser() {
        Organization organization = new Organization();
        organization.setName("Evaluación manual Gemini");
        organization = organizationRepository.save(organization);
        Usuario user = new Usuario();
        user.setNombre("Evaluación manual");
        user.setEmail("presupuesto-ia-eval@test.local");
        user.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        user.setRol("USER");
        user.setActivo(true);
        user.setOrganization(organization);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        return usuarioRepository.save(user);
    }

    private void saveMaterials(Usuario user) {
        for (String[] row : List.of(
                new String[]{"Plato de ducha resina 120x70 blanco liso", "ud", "310.00"},
                new String[]{"Plato de ducha resina 100x70 textura pizarra", "ud", "285.00"},
                new String[]{"Alicatado cerámico pared blanco mate 30x60", "m2", "24.50"},
                new String[]{"Gres porcelánico suelo 60x60 antideslizante", "m2", "31.00"},
                new String[]{"Laminado roble claro AC4 8 mm", "m2", "19.90"},
                new String[]{"Laminado roble natural AC5 12 mm", "m2", "29.90"},
                new String[]{"Rodapié MDF blanco 70 mm", "ml", "5.50"},
                new String[]{"Rodapié PVC nogal 90 mm", "ml", "7.25"},
                new String[]{"Mortero de albañilería M-5", "saco", "8.90"},
                new String[]{"Pintura plástica exterior blanca mate 15 L", "ud", "48.00"},
                new String[]{"Pintura plástica interior blanca mate 15 L", "ud", "39.00"},
                new String[]{"Esmalte sintético blanco satinado 750 ml", "ud", "13.50"},
                new String[]{"Base enchufe doble blanco serie Basic", "ud", "7.00"},
                new String[]{"Base enchufe simple blanco serie Basic", "ud", "4.00"},
                new String[]{"Tubo PVC evacuación Ø40 mm", "ml", "3.25"},
                new String[]{"Tubo PVC evacuación Ø32 mm", "ml", "2.85"},
                new String[]{"Radiador aluminio blanco 12 elementos", "ud", "105.00"},
                new String[]{"Radiador aluminio blanco 10 elementos", "ud", "85.00"},
                new String[]{"Placa de yeso laminado 13 mm", "m2", "15.00"})) {
            Material material = new Material();
            material.setUsuario(user);
            material.setNombre(row[0]);
            material.setUnidadMedida(row[1]);
            material.setPrecioUnitario(Double.valueOf(row[2]));
            materialRepository.save(material);
        }
    }
}
