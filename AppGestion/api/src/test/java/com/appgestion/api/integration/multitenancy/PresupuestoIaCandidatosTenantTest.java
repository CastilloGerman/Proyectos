package com.appgestion.api.integration.multitenancy;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Material;
import com.appgestion.api.domain.entity.Organization;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.service.GeminiClient;
import com.appgestion.api.service.GeminiGenerationResult;
import com.appgestion.api.service.PresupuestoIaService;
import com.appgestion.api.dto.request.PresupuestoIaRequest;
import com.appgestion.api.repository.MaterialRepository;
import com.appgestion.api.repository.OrganizationRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
class PresupuestoIaCandidatosTenantTest {

    @Autowired private PresupuestoIaService presupuestoIaService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private GeminiClient geminiClient;

    private Usuario authenticatedUser;
    private Material ownMaterial;

    @BeforeEach
    void setUp() throws Exception {
        authenticatedUser = createUser("ia-candidatos-a@test.local", "Org IA A");
        Usuario otherUser = createUser("ia-candidatos-b@test.local", "Org IA B");
        ownMaterial = materialRepository.save(material(authenticatedUser, "Azulejo blanco", 24.0));
        materialRepository.save(material(otherUser, "Azulejo exclusivo de otro usuario", 99.0));

        when(geminiClient.generateWithMetadata(anyString(), anyString(), isNull(), isNull(), any(JsonNode.class),
                eq(JsonNode.class), any(Runnable.class))).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(1);
            assertTrue(prompt.contains("Azulejo blanco"));
            assertFalse(prompt.contains("Azulejo exclusivo de otro usuario"));
            return new GeminiGenerationResult<>(objectMapper.readTree("""
                    {"transcripcion":"alicatar","partidas":[{"descripcion":"Alicatar baÃ±o","cantidad":4,
                     "unidad":"m2","materialId":%d,"confianza":"alta"}],"notas":null}
                    """.formatted(ownMaterial.getId())), 35, 12);
        });
    }

    @Test
    void sendsOnlyAuthenticatedUsersMaterialsAsCandidates() {
        var response = presupuestoIaService.generarBorrador(new PresupuestoIaRequest("alicatar azulejo", null), authenticatedUser);
        assertTrue(response.items().getFirst().materialId().equals(ownMaterial.getId()));
        verify(geminiClient).generateWithMetadata(anyString(), anyString(), isNull(), isNull(), any(JsonNode.class),
                eq(JsonNode.class), any(Runnable.class));
    }

    private Usuario createUser(String email, String organizationName) {
        Organization organization = new Organization();
        organization.setName(organizationName);
        organization = organizationRepository.save(organization);
        Usuario user = new Usuario();
        user.setNombre("Usuario integraciÃ³n IA");
        user.setEmail(email);
        user.setPasswordHash("$2a$10$dummyhashfordummytestsxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        user.setRol("USER");
        user.setActivo(true);
        user.setOrganization(organization);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        return usuarioRepository.save(user);
    }

    private static Material material(Usuario user, String name, double price) {
        Material material = new Material();
        material.setUsuario(user);
        material.setNombre(name);
        material.setUnidadMedida("m2");
        material.setPrecioUnitario(price);
        return material;
    }
}
