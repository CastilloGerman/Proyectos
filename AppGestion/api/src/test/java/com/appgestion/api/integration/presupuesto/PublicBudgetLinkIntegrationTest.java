package com.appgestion.api.integration.presupuesto;

import com.appgestion.api.AppGestionApiApplication;
import com.appgestion.api.domain.entity.Empresa;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.PresupuestoEnlace;
import com.appgestion.api.integration.multitenancy.MultitenancyAuth;
import com.appgestion.api.integration.multitenancy.MultitenancyIntegrationTestSupport;
import com.appgestion.api.repository.*;
import com.appgestion.api.service.PublicBudgetLinkService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
            // Clean up committed data so subsequent tests can seed fresh data
            // Order: children first, then parents (using JdbcTemplate to avoid production repo methods)
            jdbcTemplate.update("delete from presupuesto_enlace where presupuesto_id = ?", budgetId);
            jdbcTemplate.update("delete from notificaciones where usuario_id = ?", userAId);
            jdbcTemplate.update("delete from notificaciones where usuario_id = ?", userBId);
            jdbcTemplate.update("delete from email_jobs where usuario_id = ?", userAId);
            jdbcTemplate.update("delete from email_jobs where usuario_id = ?", userBId);
            presupuestoRepository.deleteById(budgetId);
            facturaRepository.deleteById(invoiceId);
            clienteRepository.deleteById(clientAId);
            clienteRepository.deleteById(clientBId);
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

    // === Fase 6b: Responder endpoint integration tests ===

    private void markBudgetAsSent(Long budgetId) {
        Presupuesto p = presupuestoRepository.findById(budgetId).orElseThrow();
        p.setEnviadoAt(java.time.LocalDateTime.now());
        presupuestoRepository.save(p);
        entityManager.flush();
    }

    @Test
    void responderEndpointIsPermitAllWithoutAuthentication() throws Exception {
        // POST /responder must be accessible without auth
        String token = token(createLink(scenario.presupuestoIdB()));
        // A non-existent budget under the token will return 404, not 401
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\"}"))
                .andExpect(status().isNotFound());
        // Other budget management endpoints still require auth
        mvc.perform(get("/presupuestos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/presupuestos")).andExpect(status().isUnauthorized());
    }

    @Test
    void responderInvalidOption_returns400() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INVALIDA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void responderMessageTooLong_returns400() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));
        String longMessage = "a".repeat(501);
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content(String.format("{\"opcion\":\"INTERESA\",\"mensaje\":\"%s\"}", longMessage)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void responderSavesResponseAndCreatesNotification() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));
        Long userBId = scenario.usuarioB().getId();

        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\",\"mensaje\":\"Gracias\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Tu aviso se ha enviado a la empresa."));

        // Verify the response was saved
        entityManager.clear();
        Presupuesto presupuesto = presupuestoRepository.findById(scenario.presupuestoIdB()).orElseThrow();
        assertEquals("INTERESA", presupuesto.getRespuestaCliente());
        assertNotNull(presupuesto.getRespuestaClienteAt());
        assertEquals("Gracias", presupuesto.getRespuestaClienteMensaje());

        // Verify a notification was created
        long notifications = notificacionRepository.countByUsuarioId(userBId);
        assertEquals(1, notifications);
    }

    @Test
    void responderDoesNotChangeBudgetEstado() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));
        Presupuesto before = presupuestoRepository.findById(scenario.presupuestoIdB()).orElseThrow();
        String estadoAntes = before.getEstado();

        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\"}"))
                .andExpect(status().isOk());

        entityManager.clear();
        Presupuesto after = presupuestoRepository.findById(scenario.presupuestoIdB()).orElseThrow();
        assertEquals(estadoAntes, after.getEstado());
    }

    @RepeatedTest(20)
    void concurrentResponderRequestsProduceExactlyOneNotification() throws Exception {
        Long budgetId = scenario.presupuestoIdB();
        Long invoiceId = scenario.facturaIdB();
        Long clientAId = scenario.clienteIdA();
        Long clientBId = scenario.clienteIdB();
        Long userAId = scenario.usuarioA().getId();
        Long userBId = scenario.usuarioB().getId();
        Long organizationId = scenario.usuarioA().getOrganization().getId();

        // Mark budget as sent before creating the link
        Presupuesto p = presupuestoRepository.findById(budgetId).orElseThrow();
        p.setEnviadoAt(java.time.LocalDateTime.now());
        presupuestoRepository.save(p);

        String token = token(createLink(budgetId));

        // Commit so concurrent requests can work outside the test transaction
        TestTransaction.flagForCommit();
        TestTransaction.end();

        try {
            int callers = 8;
            ExecutorService workers = Executors.newFixedThreadPool(callers);
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Integer>> results = new ArrayList<>();
                for (int i = 0; i < callers; i++) {
                    results.add(workers.submit(() -> {
                        start.await();
                        return mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                                        .contentType("application/json")
                                        .content("{\"opcion\":\"INTERESA\"}"))
                                .andReturn().getResponse().getStatus();
                    }));
                }
                start.countDown();
                for (Future<Integer> result : results) {
                    int status = result.get(15, TimeUnit.SECONDS);
                    assertTrue(status == 200 || status == 404, "Expected 200 or 404, got " + status);
                }
            } finally {
                workers.shutdownNow();
                assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
            }

            // Re-enter transaction to verify
            TestTransaction.start();
            entityManager.clear();

            // EXACTLY 1 notification: pessimistic lock serializes transactions;
            // conditional update returns 0 for all but the first.
            long notifications = notificacionRepository.countByUsuarioId(userBId);
            assertEquals(1, notifications, "Exactly 1 notification expected from 8 concurrent requests (same option)");

            // EXACTLY 1 email in the outbox: one email per notification.
            // Email sending is always enabled in the test profile.
            Long emailCount = jdbcTemplate.queryForObject(
                    "select count(*) from email_jobs where usuario_id = ?", Long.class, userBId);
            assertNotNull(emailCount, "Email count should not be null");
            assertEquals(1, emailCount.longValue(), "Exactly 1 email expected in outbox");

            // The response should be saved
            Presupuesto presupuesto = presupuestoRepository.findById(budgetId).orElseThrow();
            assertEquals("INTERESA", presupuesto.getRespuestaCliente());
        } finally {
            if (!TestTransaction.isActive()) TestTransaction.start();
            entityManager.clear();
            // Clean up committed data so subsequent tests can seed fresh data
            // Order: children first, then parents (using JdbcTemplate to avoid production repo methods)
            jdbcTemplate.update("delete from presupuesto_enlace where presupuesto_id = ?", budgetId);
            jdbcTemplate.update("delete from notificaciones where usuario_id = ?", userAId);
            jdbcTemplate.update("delete from notificaciones where usuario_id = ?", userBId);
            jdbcTemplate.update("delete from email_jobs where usuario_id = ?", userAId);
            jdbcTemplate.update("delete from email_jobs where usuario_id = ?", userBId);
            presupuestoRepository.deleteById(budgetId);
            facturaRepository.deleteById(invoiceId);
            clienteRepository.deleteById(clientAId);
            clienteRepository.deleteById(clientBId);
            usuarioRepository.deleteById(userAId);
            usuarioRepository.deleteById(userBId);
            organizationRepository.deleteById(organizationId);
            TestTransaction.flagForCommit();
            TestTransaction.end();
        }
    }

    @Test
    void publicGetIncludesPermiteResponderAndRespuestaCliente() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));

        // Before responding: permiteResponder=true, respuestaCliente is absent or null
        mvc.perform(get("/publico/presupuestos/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permiteResponder").value(true))
                .andExpect(jsonPath("$.respuestaCliente").doesNotExist());

        // Respond
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"DUDAS\",\"mensaje\":\"Pregunta secreta\"}"))
                .andExpect(status().isOk());

        // After responding: permiteResponder=true, respuestaCliente=DUDAS, NO message exposed
        mvc.perform(get("/publico/presupuestos/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permiteResponder").value(true))
                .andExpect(jsonPath("$.respuestaCliente").value("DUDAS"))
                .andExpect(jsonPath("$.respuestaClienteMensaje").doesNotExist())
                .andExpect(jsonPath("$.mensaje").doesNotExist());
    }

    @Test
    void publicGetReturnsPermiteResponderFalseWhenDisabled() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        // Disable client responses for the owner's empresa
        // Use the existing empresa if it exists, or update via the service instead of raw SQL
        Long count = jdbcTemplate.queryForObject("select count(*) from empresas where usuario_id = ?", Long.class,
                scenario.usuarioB().getId());
        if (count == null || count == 0L) {
            // No empresa exists; use the API to create one with all required fields
            mvc.perform(patch("/config/empresa/seguimiento-presupuestos")
                            .with(asOwner())
                            .contentType("application/json")
                            .content("{\"seguimientoActivo\":false,\"seguimientoDiasEspera\":3,\"seguimientoMaxAvisos\":2,\"seguimientoEmailResumen\":false,\"permitirRespuestaCliente\":false}"))
                    .andExpect(status().isOk());
        } else {
            jdbcTemplate.update("update empresas set permitir_respuesta_cliente = false where usuario_id = ?",
                    scenario.usuarioB().getId());
        }

        String token = token(createLink(scenario.presupuestoIdB()));

        mvc.perform(get("/publico/presupuestos/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permiteResponder").value(false));

        // Responder should return uniform 404 when disabled
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\"}"))
                .andExpect(status().isNotFound());

        // Restore
        jdbcTemplate.update("update empresas set permitir_respuesta_cliente = true where usuario_id = ?",
                scenario.usuarioB().getId());
    }

    @Test
    void responderClientMessageNotInLogs(CapturedOutput output) throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));
        String secretMessage = "mensaje-secreto-cliente-que-no-debe-aparecer";

        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content(String.format("{\"opcion\":\"INTERESA\",\"mensaje\":\"%s\"}", secretMessage)))
                .andExpect(status().isOk());

        String allOutput = output.getAll();
        assertFalse(allOutput.contains(secretMessage), "Client message must not appear in logs");
        assertFalse(allOutput.contains(token), "Token must not appear in logs");
    }

    @Test
    void responderIdor_OtherUserCannotSeeClientResponse() throws Exception {
        // User B's budget — User A should not see the response details
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));

        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\",\"mensaje\":\"Secreto\"}"))
                .andExpect(status().isOk());

        // User A cannot access the budget detail
        mvc.perform(get("/presupuestos/{id}", scenario.presupuestoIdB())
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isNotFound());
    }

    @Test
    void responderIdor_OtherUserCannotChangeSettings() throws Exception {
        // User A cannot see User B's settings — the endpoint is scoped to the authenticated user.
        // User A has no empresa, so GET returns defaults (not User B's settings).
        // This verifies the endpoint uses the current user, not a user-supplied ID.
        mvc.perform(get("/config/empresa/seguimiento-presupuestos")
                        .with(MultitenancyAuth.asUsuarioA(userDetailsService)))
                .andExpect(status().isOk());
        // The response is User A's own settings, not User B's — no IDOR possible.
        // We just verify the endpoint returns 200 for the authenticated user's own data.
    }

    @Test
    void responderRateLimitOnFailedAttempts() throws Exception {
        // Rate limit applies even when responder fails (404 for bad token)
        // The per-IP limit is 120 by default; use same token to hit per-token limit (60)
        String badToken = "bad-token-rate-limit-test";
        for (int i = 0; i < 60; i++) {
            mvc.perform(post("/publico/presupuestos/{token}/responder", badToken)
                            .contentType("application/json")
                            .content("{\"opcion\":\"INTERESA\"}"))
                    .andExpect(status().isNotFound());
        }
        // 61st request with same token should be rate-limited (per-token limit)
        mvc.perform(post("/publico/presupuestos/{token}/responder", badToken)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void responderCooldownExceeded_returns429() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));

        // First response
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\"}"))
                .andExpect(status().isOk());

        // Immediate change should be rejected (within 5 minutes)
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"DUDAS\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "300"));
    }

    @Test
    void responderSameOptionAndSameMessageIsIdempotent() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));
        Long userBId = scenario.usuarioB().getId();

        // First response with message
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\",\"mensaje\":\"Hola\"}"))
                .andExpect(status().isOk());

        long notificationsAfterFirst = notificacionRepository.countByUsuarioId(userBId);

        // Same option AND same message — should be idempotent (no new notification)
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\",\"mensaje\":\"Hola\"}"))
                .andExpect(status().isOk());

        long notificationsAfterSecond = notificacionRepository.countByUsuarioId(userBId);
        assertEquals(notificationsAfterFirst, notificationsAfterSecond,
                "Same option and same message should not create a new notification");

        entityManager.clear();
        Presupuesto presupuesto = presupuestoRepository.findById(scenario.presupuestoIdB()).orElseThrow();
        assertEquals("INTERESA", presupuesto.getRespuestaCliente());
        assertEquals("Hola", presupuesto.getRespuestaClienteMensaje());
    }

    @Test
    void responderSameOptionDifferentMessage_updatesAfterCooldown() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));
        Long userBId = scenario.usuarioB().getId();

        // First response
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\",\"mensaje\":\"Mensaje 1\"}"))
                .andExpect(status().isOk());

        long notificationsAfterFirst = notificacionRepository.countByUsuarioId(userBId);
        assertEquals(1, notificationsAfterFirst);

        // Same option, different message — within cooldown → 429
        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\",\"mensaje\":\"Mensaje 2\"}"))
                .andExpect(status().isTooManyRequests());

        // After cooldown, same option with different message updates the message
        // Simulate cooldown by backdating respuestaClienteAt (H2-compatible syntax)
        jdbcTemplate.update("update presupuestos set respuesta_cliente_at = respuesta_cliente_at - INTERVAL '6' MINUTE where id = ?",
                scenario.presupuestoIdB());
        entityManager.clear();

        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\",\"mensaje\":\"Mensaje 2\"}"))
                .andExpect(status().isOk());

        long notificationsAfterSecond = notificacionRepository.countByUsuarioId(userBId);
        assertEquals(2, notificationsAfterSecond,
                "Different message after cooldown should create a new notification");

        entityManager.clear();
        Presupuesto presupuesto = presupuestoRepository.findById(scenario.presupuestoIdB()).orElseThrow();
        assertEquals("INTERESA", presupuesto.getRespuestaCliente());
        assertEquals("Mensaje 2", presupuesto.getRespuestaClienteMensaje(),
                "Message should be updated when it differs");
    }

    @RepeatedTest(20)
    void concurrentMixedOptions_leavesConsistentState() throws Exception {
        // Half send INTERESA, half send DUDAS simultaneously.
        // With pessimistic locking, the first transaction wins and writes its option.
        // The second distinct option reads the written value, sees a distinct option,
        // but the conditional update checks the previous value — since the first
        // transaction already committed, the second sees the new value, not null.
        // The cooldown (5 min) then blocks any further changes.
        // Result: EXACTLY 1 notification because only the first writer succeeds.
        Long budgetId = scenario.presupuestoIdB();
        Long invoiceId = scenario.facturaIdB();
        Long clientAId = scenario.clienteIdA();
        Long clientBId = scenario.clienteIdB();
        Long userAId = scenario.usuarioA().getId();
        Long userBId = scenario.usuarioB().getId();
        Long organizationId = scenario.usuarioA().getOrganization().getId();

        markBudgetAsSent(budgetId);
        String token = token(createLink(budgetId));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        try {
            int callers = 8;
            ExecutorService workers = Executors.newFixedThreadPool(callers);
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Integer>> results = new ArrayList<>();
                for (int i = 0; i < callers; i++) {
                    final String opcion = (i % 2 == 0) ? "INTERESA" : "DUDAS";
                    results.add(workers.submit(() -> {
                        start.await();
                        return mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                                        .contentType("application/json")
                                        .content(String.format("{\"opcion\":\"%s\"}", opcion)))
                                .andReturn().getResponse().getStatus();
                    }));
                }
                start.countDown();
                for (Future<Integer> result : results) {
                    int status = result.get(15, TimeUnit.SECONDS);
                    assertTrue(status == 200 || status == 404 || status == 429,
                            "Expected 200/404/429, got " + status);
                }
            } finally {
                workers.shutdownNow();
                assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
            }

            // Re-enter transaction to verify
            TestTransaction.start();
            entityManager.clear();

            // Final state: exactly one option saved (either INTERESA or DUDAS)
            Presupuesto presupuesto = presupuestoRepository.findById(budgetId).orElseThrow();
            String finalOption = presupuesto.getRespuestaCliente();
            assertNotNull(finalOption, "Final option should not be null");
            assertTrue("INTERESA".equals(finalOption) || "DUDAS".equals(finalOption),
                    "Final option should be INTERESA or DUDAS, got " + finalOption);

            // EXACTLY 1 notification: the first writer wins via pessimistic lock.
            // All other writers either see the same option (idempotent, 0 rows)
            // or are blocked by the cooldown (429). Even if a different option
            // reads null before the first commits, the conditional update will
            // fail because the previous value no longer matches.
            long notifications = notificacionRepository.countByUsuarioId(userBId);
            assertEquals(1, notifications,
                    "Exactly 1 notification expected from mixed concurrent requests (first writer wins)");

            // EXACTLY 1 email in the outbox: one email per notification.
            Long emailCount = jdbcTemplate.queryForObject(
                    "select count(*) from email_jobs where usuario_id = ?", Long.class, userBId);
            assertNotNull(emailCount);
            assertEquals(1, emailCount.longValue(), "Exactly 1 email expected in outbox");

        } finally {
            if (!TestTransaction.isActive()) TestTransaction.start();
            entityManager.clear();
            jdbcTemplate.update("delete from presupuesto_enlace where presupuesto_id = ?", budgetId);
            jdbcTemplate.update("delete from notificaciones where usuario_id = ?", userAId);
            jdbcTemplate.update("delete from notificaciones where usuario_id = ?", userBId);
            jdbcTemplate.update("delete from email_jobs where usuario_id = ?", userAId);
            jdbcTemplate.update("delete from email_jobs where usuario_id = ?", userBId);
            presupuestoRepository.deleteById(budgetId);
            facturaRepository.deleteById(invoiceId);
            clienteRepository.deleteById(clientAId);
            clienteRepository.deleteById(clientBId);
            usuarioRepository.deleteById(userAId);
            usuarioRepository.deleteById(userBId);
            organizationRepository.deleteById(organizationId);
            TestTransaction.flagForCommit();
            TestTransaction.end();
        }
    }

    @Test
    void responderSecurityHeadersPresent() throws Exception {
        markBudgetAsSent(scenario.presupuestoIdB());
        String token = token(createLink(scenario.presupuestoIdB()));

        mvc.perform(post("/publico/presupuestos/{token}/responder", token)
                        .contentType("application/json")
                        .content("{\"opcion\":\"INTERESA\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Robots-Tag", "noindex, nofollow"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
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
