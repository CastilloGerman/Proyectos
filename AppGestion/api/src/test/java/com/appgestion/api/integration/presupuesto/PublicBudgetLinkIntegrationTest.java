package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.PresupuestoEnlace;
import com.appgestion.api.integration.multitenancy.MultitenancyAuth;
import com.appgestion.api.integration.multitenancy.MultitenancyIntegrationTestSupport;
import com.appgestion.api.repository.*;
import com.appgestion.api.service.PublicBudgetLinkService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.TestTransaction;
import jakarta.persistence.EntityManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest(classes = AppGestionApiApplication.class)
@ActiveProfiles("test")
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class PublicBudgetLinkIntegrationTest {
    @Autowired private WebApplicationContext context;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private PresupuestoRepository presupuestoRepository;
    @Autowired private PresupuestoEnlaceRepository enlaceRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;
    private MockMvc mvc;
    private MultitenancyIntegrationTestSupport.Scenario scenario;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        scenario = MultitenancyIntegrationTestSupport.seed(organizationRepository, usuarioRepository,
                clienteRepository, facturaRepository, presupuestoRepository);
    }

    @Test
    void ownerCanCreateAndRegenerateButOtherUserCannotReadStateOrRevoke() throws Exception {
        JsonNode first = createLink(scenario.presupuestoIdB());
        String firstToken = token(first);
        var deniedCreate = mvc.perform(post("/presupuestos/{id}/enlace", scenario.presupuestoIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound()).andReturn();
        assertNotNull(deniedCreate);
        mvc.perform(get("/presupuestos/{id}/enlace/estado", scenario.presupuestoIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/presupuestos/{id}/enlace", scenario.presupuestoIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());

        JsonNode second = createLink(scenario.presupuestoIdB());
        assertNotEquals(firstToken, token(second));
        assertTrue(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                PublicBudgetLinkService.hash(firstToken), java.time.Instant.now()).isPresent());
        assertTrue(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                PublicBudgetLinkService.hash(token(second)), java.time.Instant.now()).isPresent());

        mvc.perform(post("/presupuestos/{id}/enlace/regenerar", scenario.presupuestoIdB()).with(asOwner()))
                .andExpect(status().isOk());
        assertTrue(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                PublicBudgetLinkService.hash(firstToken), java.time.Instant.now()).isEmpty());
        assertTrue(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                PublicBudgetLinkService.hash(token(second)), java.time.Instant.now()).isEmpty());
        JsonNode third = createLink(scenario.presupuestoIdB());
        mvc.perform(get("/publico/presupuestos/{token}/pdf", token(third)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"presupuesto.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/publico/presupuestos/{token}", firstToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Este enlace no está disponible"));
        mvc.perform(get("/publico/presupuestos/{token}", token(third))).andExpect(status().isOk());
    }

    @Test
    void deletingBudgetRemovesItsPublicLink() throws Exception {
        String token = token(createLink(scenario.presupuestoIdB()));
        presupuestoRepository.deleteById(scenario.presupuestoIdB());
        entityManager.flush();
        entityManager.clear();

        assertTrue(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                PublicBudgetLinkService.hash(token), java.time.Instant.now()).isEmpty());
        mvc.perform(get("/publico/presupuestos/{token}", token)).andExpect(status().isNotFound());
    }

    @Test
    void createdLinkPersistsOnlyTheTokenHash() throws Exception {
        String token = token(createLink(scenario.presupuestoIdB()));
        String hash = PublicBudgetLinkService.hash(token);
        PresupuestoEnlace link = enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                hash, java.time.Instant.now()).orElseThrow();
        var storedRow = jdbcTemplate.queryForMap(
                "select * from presupuesto_enlace where id = ?", link.getId());
        String storedHash = (String) storedRow.get("token_hash");

        assertEquals(hash, storedHash);
        assertFalse(storedRow.values().contains(token));
        assertEquals(1, storedRow.keySet().stream()
                .filter(column -> column.toLowerCase(java.util.Locale.ROOT).contains("token")).count());
        assertTrue(storedHash.matches("[0-9a-f]{64}"));
    }

    @Test
    void concurrentViewedPostsPersistOneFirstViewNotification() throws Exception {
        Long budgetId = scenario.presupuestoIdB();
        Long invoiceId = scenario.facturaIdB();
        Long clientAId = scenario.clienteIdA();
        Long clientBId = scenario.clienteIdB();
        Long userAId = scenario.usuarioA().getId();
        Long userBId = scenario.usuarioB().getId();
        Long organizationId = scenario.usuarioA().getOrganization().getId();
        String token = token(createLink(budgetId));
        String hash = PublicBudgetLinkService.hash(token);
        TestTransaction.flagForCommit();
        TestTransaction.end();

        try {
            int callers = 10;
            ExecutorService workers = Executors.newFixedThreadPool(callers);
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Integer>> results = new ArrayList<>();
                for (int i = 0; i < callers; i++) {
                    results.add(workers.submit(() -> {
                        start.await();
                        return mvc.perform(post("/publico/presupuestos/{token}/visto", token))
                                .andReturn().getResponse().getStatus();
                    }));
                }
                start.countDown();
                for (Future<Integer> result : results) assertEquals(204, result.get(10, TimeUnit.SECONDS));
            } finally {
                workers.shutdownNow();
                assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
            }

            TestTransaction.start();
            entityManager.clear();
            PresupuestoEnlace link = enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                    hash, java.time.Instant.now()).orElseThrow();
            assertEquals(1, link.getNumVistas());
            assertNotNull(link.getPrimeraVistaAt());
            assertEquals(1, notificacionRepository.countByUsuarioId(userBId));
        } finally {
            if (!TestTransaction.isActive()) TestTransaction.start();
            entityManager.clear();
            presupuestoRepository.deleteById(budgetId);
            facturaRepository.deleteById(invoiceId);
            clienteRepository.deleteById(clientAId);
            clienteRepository.deleteById(clientBId);
            notificacionRepository.deleteByUsuarioId(userAId);
            notificacionRepository.deleteByUsuarioId(userBId);
            usuarioRepository.deleteById(userAId);
            usuarioRepository.deleteById(userBId);
            organizationRepository.deleteById(organizationId);
            TestTransaction.flagForCommit();
            TestTransaction.end();
        }
    }

    @Test
    void publicGetDoesNotRecordViewAndPostDoesOnceWithSafeDtoAndHeaders() throws Exception {
        String token = token(createLink(scenario.presupuestoIdB()));
        String secondToken = token(createLink(scenario.presupuestoIdB()));
        String hash = PublicBudgetLinkService.hash(token);
        PresupuestoEnlace link = enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(
                hash, java.time.Instant.now()).orElseThrow();

        mvc.perform(get("/publico/presupuestos/{token}", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.clienteNombre").value("Cliente de B"))
                .andExpect(jsonPath("$.dni").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.direccion").doesNotExist());
        assertNull(link.getPrimeraVistaAt());

        mvc.perform(post("/publico/presupuestos/{token}/visto", token)).andExpect(status().isNoContent());
        mvc.perform(post("/publico/presupuestos/{token}/visto", token)).andExpect(status().isNoContent());
        mvc.perform(post("/publico/presupuestos/{token}/visto", secondToken)).andExpect(status().isNoContent());
        entityManager.clear();
        PresupuestoEnlace updated = enlaceRepository.findById(link.getId()).orElseThrow();
        assertNotNull(updated.getPrimeraVistaAt());
        assertEquals(1, updated.getNumVistas());
        assertEquals(1, notificacionRepository.countByUsuarioId(scenario.usuarioB().getId()));
        var statusJson = mvc.perform(get("/presupuestos/{id}/enlace/estado", scenario.presupuestoIdB()).with(asOwner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numVistas").value(2))
                .andExpect(jsonPath("$.enlacesActivos").value(2))
                .andReturn().getResponse().getContentAsString();
        assertNotNull(statusJson);
        mvc.perform(get("/presupuestos/{id}", scenario.presupuestoIdB()).with(asOwner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enlaceNumVistas").value(2))
                .andExpect(jsonPath("$.enlacePrimeraVistaAt").isNotEmpty());
    }

    @Test
    void publicPathsAreAnonymousButBudgetManagementStillRequiresAuthentication() throws Exception {
        mvc.perform(get("/publico/presupuestos/unknown-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Este enlace no está disponible"));
        mvc.perform(get("/publico/presupuestos/unknown-token/internal")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/publico/presupuestos/unknown-token")).andExpect(status().isUnauthorized());
        mvc.perform(get("/presupuestos/1/enlace/estado")).andExpect(status().isUnauthorized());
    }

    @Test
    void activeLinkLimitIsClearAndSendingDoesNotInvalidateEarlierLinks() throws Exception {
        for (int i = 0; i < 10; i++) createLink(scenario.presupuestoIdB());
        mvc.perform(post("/presupuestos/{id}/enlace", scenario.presupuestoIdB()).with(asOwner()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("máximo de enlaces activos")));
        var status = mvc.perform(get("/presupuestos/{id}/enlace/estado", scenario.presupuestoIdB()).with(asOwner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enlacesActivos").value(10))
                .andReturn();
        assertNotNull(status);
    }

    @Test
    void invalidExpiredAndRevokedTokensHaveSameResponse() throws Exception {
        PresupuestoEnlace expired = new PresupuestoEnlace();
        expired.setPresupuesto(presupuestoRepository.findById(scenario.presupuestoIdB()).orElseThrow());
        expired.setTokenHash(PublicBudgetLinkService.hash("expired-token-for-test"));
        expired.setCreadoAt(java.time.Instant.now().minusSeconds(7200));
        expired.setExpiraAt(java.time.Instant.now().minusSeconds(3600));
        expired.setRevocado(false);
        enlaceRepository.save(expired);

        PresupuestoEnlace revoked = new PresupuestoEnlace();
        revoked.setPresupuesto(expired.getPresupuesto());
        revoked.setTokenHash(PublicBudgetLinkService.hash("revoked-token-for-test"));
        revoked.setCreadoAt(java.time.Instant.now());
        revoked.setExpiraAt(java.time.Instant.now().plusSeconds(3600));
        revoked.setRevocado(true);
        enlaceRepository.save(revoked);
        entityManager.flush();

        String unknown = mvc.perform(get("/publico/presupuestos/{token}", "missing-token-for-test"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String old = mvc.perform(get("/publico/presupuestos/{token}", "expired-token-for-test"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String blocked = mvc.perform(get("/publico/presupuestos/{token}", "revoked-token-for-test"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertEquals(unknown, old);
        assertEquals(unknown, blocked);
    }

    @Test
    void tokenAndPublicUrlAreNotWrittenToApplicationLogs(CapturedOutput output) throws Exception {
        String token = "opaque-secret-token-that-must-not-appear";
        mvc.perform(get("/publico/presupuestos/{token}", token)).andExpect(status().isNotFound());
        assertFalse(output.getAll().contains(token));
        assertFalse(output.getAll().contains("/publico/presupuestos/" + token));
    }

    @Test
    void trustedProxyAllowsManyDistinctForwardedClientsWithoutSharedIpThrottling() throws Exception {
        for (int i = 1; i <= 130; i++) {
            final int client = i;
            mvc.perform(get("/publico/presupuestos/{token}", "invalid-" + i)
                            .with(request -> {
                                request.setRemoteAddr("127.0.0.1");
                                request.addHeader("X-Forwarded-For", "198.51.100." + client);
                                return request;
                            }))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void excessiveRequestsForOneTokenAreRateLimited() throws Exception {
        String token = token(createLink(scenario.presupuestoIdB()));
        for (int i = 0; i < 60; i++) {
            mvc.perform(post("/publico/presupuestos/{token}/visto", token)).andExpect(status().isNoContent());
        }
        mvc.perform(post("/publico/presupuestos/{token}/visto", token)).andExpect(status().isTooManyRequests());
    }

    private JsonNode createLink(Long budgetId) throws Exception {
        String json = mvc.perform(post("/presupuestos/{id}/enlace", budgetId)
                        .with(asOwner()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static String token(JsonNode response) {
        String url = response.path("url").asText();
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private RequestPostProcessor asOwner() {
        var user = userDetailsService.loadUserByUsername(MultitenancyIntegrationTestSupport.EMAIL_USUARIO_B);
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
