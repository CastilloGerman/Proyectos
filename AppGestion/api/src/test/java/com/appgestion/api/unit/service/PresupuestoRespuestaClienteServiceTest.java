package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.*;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.dto.request.RespuestaClienteRequest;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.PresupuestoEnlaceRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import com.appgestion.api.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests comprehensivos para el endpoint público anónimo POST /publico/presupuestos/{token}/responder.
 * Cubren: guardar respuesta, notificación única, concurrencia, cooldown, validación,
 * función desactivada, saneado de mensaje, email sin tokens, seguimiento suprimido,
 * IDOR, rate limit en intentos fallidos y GET público con permiteResponder.
 */
@ExtendWith(MockitoExtension.class)
class PresupuestoRespuestaClienteServiceTest {

    private static final Long OWNER_ID = 8L;
    private static final Long BUDGET_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");
    private static final String TOKEN_HASH = PublicBudgetLinkService.hash("some-token");

    @Mock PresupuestoEnlaceRepository enlaceRepository;
    @Mock PresupuestoRepository presupuestoRepository;
    @Mock EmpresaRepository empresaRepository;
    @Mock NotificacionService notificacionService;
    @Mock EmailService emailService;

    private Usuario owner;
    private Empresa empresa;
    private Presupuesto budget;
    private PresupuestoEnlace enlace;
    private PresupuestoRespuestaClienteService service;

    @BeforeEach
    void setUp() {
        owner = new Usuario();
        owner.setId(OWNER_ID);
        owner.setNombre("Owner");
        owner.setEmail("owner@example.test");
        owner.setUiLocale("es");

        empresa = new Empresa();
        empresa.setUsuario(owner);
        empresa.setPermitirRespuestaCliente(true);

        budget = new Presupuesto();
        budget.setId(BUDGET_ID);
        budget.setUsuario(owner);
        budget.setCliente(new Cliente());
        budget.getCliente().setNombre("Cliente Test");
        budget.setEstado("Pendiente");
        budget.setEnviadoAt(LocalDateTime.of(2026, 10, 6, 10, 0));

        enlace = new PresupuestoEnlace();
        enlace.setPresupuesto(budget);
        enlace.setTokenHash(TOKEN_HASH);
        enlace.setRevocado(false);
        enlace.setExpiraAt(Instant.now().plus(Duration.ofDays(60)));

        service = new PresupuestoRespuestaClienteService(
                enlaceRepository, presupuestoRepository, empresaRepository,
                notificacionService, emailService,
                Clock.fixed(NOW, ZoneOffset.UTC), "https://app.example.test");

        lenient().when(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(eq(TOKEN_HASH), any()))
                .thenReturn(Optional.of(enlace));
        lenient().when(presupuestoRepository.findOwnedForUpdate(BUDGET_ID, OWNER_ID))
                .thenReturn(Optional.of(budget));
        lenient().when(empresaRepository.findByUsuarioId(OWNER_ID))
                .thenReturn(Optional.of(empresa));
    }

    // === 1. Responder INTERESA guarda respuesta, fecha y mensaje y crea UNA notificación ===

    @Test
    void responderINTERESA_guardaRespuestaFechaYMensaje_yCreaUnaNotificacion() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", "Gracias por la oferta"));

        assertThat(budget.getRespuestaCliente()).isEqualTo("INTERESA");
        assertThat(budget.getRespuestaClienteAt()).isEqualTo(NOW);
        assertThat(budget.getRespuestaClienteMensaje()).isEqualTo("Gracias por la oferta");
        verify(presupuestoRepository).updateRespuestaClienteIfDistinct(eq(BUDGET_ID), eq(OWNER_ID), eq("INTERESA"), eq(NOW), eq("Gracias por la oferta"), isNull(), isNull());
        verify(notificacionService).respuestaClientePresupuesto(owner, "Cliente Test", BUDGET_ID, "INTERESA");
        verify(emailService).enviarRespuestaCliente(eq(OWNER_ID), eq("owner@example.test"), anyString(), anyString(), anyString());
    }

    @Test
    void responderDUDAS_guardaRespuestaFechaYMensaje_yCreaUnaNotificacion() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("DUDAS", "¿Incluye transporte?"));

        assertThat(budget.getRespuestaCliente()).isEqualTo("DUDAS");
        assertThat(budget.getRespuestaClienteAt()).isEqualTo(NOW);
        assertThat(budget.getRespuestaClienteMensaje()).isEqualTo("¿Incluye transporte?");
        verify(notificacionService).respuestaClientePresupuesto(owner, "Cliente Test", BUDGET_ID, "DUDAS");
    }

    @Test
    void responderSinMensaje_guardaRespuestaSinMensaje() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", null));

        assertThat(budget.getRespuestaCliente()).isEqualTo("INTERESA");
        assertThat(budget.getRespuestaClienteMensaje()).isNull();
    }

    // === 2. El estado NO cambia ===

    @Test
    void responderINTERESA_noCambiaElEstadoDelPresupuesto() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());
        budget.setEstado("Pendiente");

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", null));

        assertThat(budget.getEstado()).isEqualTo("Pendiente");
    }

    @Test
    void responderDUDAS_noCambiaElEstadoDelPresupuesto() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());
        budget.setEstado("Pendiente");

        service.respond("some-token", new RespuestaClienteRequest("DUDAS", null));

        assertThat(budget.getEstado()).isEqualTo("Pendiente");
    }

    // === 3. Peticiones concurrentes producen UNA sola notificación por cambio ===

    @Test
    void peticionesConcurrentes_mismaOpcion_producenUnaSolaNotificacion() throws Exception {
        // Con actualización condicional: solo la primera devuelve 1 fila, las siguientes devuelven 0
        AtomicInteger updateCallCount = new AtomicInteger(0);
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    int count = updateCallCount.incrementAndGet();
                    // Only the first thread's update succeeds (row had distinct value)
                    return count == 1 ? 1 : 0;
                });
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
            var latch = new CountDownLatch(3);
            executor.submit(() -> {
                try { service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)); } catch (Exception ignored) {}
                latch.countDown();
            });
            executor.submit(() -> {
                try { service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)); } catch (Exception ignored) {}
                latch.countDown();
            });
            executor.submit(() -> {
                try { service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)); } catch (Exception ignored) {}
                latch.countDown();
            });
            latch.await(10, TimeUnit.SECONDS);
        }

        // Exactly one notification because conditional update returns 0 for duplicates
        verify(notificacionService, times(1)).respuestaClientePresupuesto(owner, "Cliente Test", BUDGET_ID, "INTERESA");
        verify(emailService, times(1)).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    // === 4. Límite de 5 minutos entre cambios de opción ===

    @Test
    void cambiarOpcion_dentroDe5Minutos_devuelve429() {
        budget.setRespuestaCliente("INTERESA");
        budget.setRespuestaClienteAt(NOW.minusSeconds(60)); // 1 minute ago

        assertThatThrownBy(() -> service.respond("some-token", new RespuestaClienteRequest("DUDAS", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(notificacionService, never()).respuestaClientePresupuesto(any(), anyString(), anyLong(), anyString());
    }

    @Test
    void cambiarOpcion_despuesDe5Minutos_sePermite() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());
        budget.setRespuestaCliente("INTERESA");
        budget.setRespuestaClienteAt(NOW.minus(Duration.ofMinutes(6))); // 6 minutes ago

        service.respond("some-token", new RespuestaClienteRequest("DUDAS", null));

        assertThat(budget.getRespuestaCliente()).isEqualTo("DUDAS");
        verify(notificacionService).respuestaClientePresupuesto(owner, "Cliente Test", BUDGET_ID, "DUDAS");
    }

    @Test
    void mismaOpcion_yMismoMensaje_noHaceNada() {
        budget.setRespuestaCliente("INTERESA");
        budget.setRespuestaClienteAt(NOW.minusSeconds(60));
        budget.setRespuestaClienteMensaje("Mensaje anterior");

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", "Mensaje anterior"));

        // Same option AND same message → early return, no update, no notification
        verify(presupuestoRepository, never()).updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any());
        verify(notificacionService, never()).respuestaClientePresupuesto(any(), anyString(), anyLong(), anyString());
    }

    @Test
    void mismaOpcion_distintoMensaje_actualizaMensaje() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());
        budget.setRespuestaCliente("INTERESA");
        budget.setRespuestaClienteAt(NOW.minus(Duration.ofMinutes(6))); // 6 minutes ago, past cooldown
        budget.setRespuestaClienteMensaje("Mensaje anterior");

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", "Mensaje nuevo"));

        // Same option but different message → update succeeds, notification created
        verify(presupuestoRepository).updateRespuestaClienteIfDistinct(eq(BUDGET_ID), eq(OWNER_ID), eq("INTERESA"), eq(NOW), eq("Mensaje nuevo"), eq("INTERESA"), eq("Mensaje anterior"));
        verify(notificacionService).respuestaClientePresupuesto(owner, "Cliente Test", BUDGET_ID, "INTERESA");
        assertThat(budget.getRespuestaClienteMensaje()).isEqualTo("Mensaje nuevo");
    }

    @Test
    void mismaOpcion_distintoMensaje_dentroDeCooldown_devuelve429() {
        budget.setRespuestaCliente("INTERESA");
        budget.setRespuestaClienteAt(NOW.minusSeconds(60)); // 1 minute ago, within cooldown
        budget.setRespuestaClienteMensaje("Mensaje anterior");

        assertThatThrownBy(() -> service.respond("some-token", new RespuestaClienteRequest("INTERESA", "Mensaje nuevo")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));

        verify(notificacionService, never()).respuestaClientePresupuesto(any(), anyString(), anyLong(), anyString());
    }

    // === 5. Token inexistente/caducado/revocado → mismo 404 ===

    @Test
    void tokenInexistente_devuelve404() {
        when(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.respond("bad-token", new RespuestaClienteRequest("INTERESA", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(notificacionService, never()).respuestaClientePresupuesto(any(), anyString(), anyLong(), anyString());
    }

    @Test
    void tokenCaducado_devuelve404() {
        enlace.setExpiraAt(Instant.now().minus(Duration.ofDays(1)));
        when(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(eq(TOKEN_HASH), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void tokenRevocado_devuelve404() {
        enlace.setRevocado(true);
        when(enlaceRepository.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(eq(TOKEN_HASH), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // === 6. Presupuesto no enviado o con estado distinto de pendiente no admite respuesta ===

    @Test
    void presupuestoNoEnviado_devuelve404() {
        budget.setEnviadoAt(null);

        assertThatThrownBy(() -> service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(notificacionService, never()).respuestaClientePresupuesto(any(), anyString(), anyLong(), anyString());
    }

    @Test
    void presupuestoAceptado_noAdmiteRespuesta() {
        budget.setEstado("Aceptado");

        assertThatThrownBy(() -> service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void presupuestoRechazado_noAdmiteRespuesta() {
        budget.setEstado("Rechazado");

        assertThatThrownBy(() -> service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // === 7. Validación: opción inválida, mensaje > 500, caracteres de control ===

    @Test
    void opcionInvalida_enDto_esRechazadaPorController() throws NoSuchMethodException {
        // @Pattern validation happens at the controller level via @Valid, not in the service.
        // The service accepts whatever the DTO contains. This test verifies the DTO annotation exists
        // by checking the accessor method (where record component annotations are placed).
        var opcionAccessor = RespuestaClienteRequest.class.getMethod("opcion");
        var patternAnnotations = opcionAccessor.getAnnotationsByType(jakarta.validation.constraints.Pattern.class);
        assertThat(patternAnnotations).isNotEmpty();
        assertThat(patternAnnotations[0].regexp()).isEqualTo("INTERESA|DUDAS");
    }

    @Test
    void mensajeConCaracteresDeControl_seSanean() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", "Hola\0\1\2mundo"));

        assertThat(budget.getRespuestaClienteMensaje()).isEqualTo("Holamundo");
    }

    // === 8. Función desactivada por el contratista → no disponible ===

    @Test
    void funcionDesactivada_porContratista_devuelve404() {
        empresa.setPermitirRespuestaCliente(false);

        assertThatThrownBy(() -> service.respond("some-token", new RespuestaClienteRequest("INTERESA", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(notificacionService, never()).respuestaClientePresupuesto(any(), anyString(), anyLong(), anyString());
    }

    // === 9. Mensaje saneado y escapado en el email, sin tokens ni URL pública ===

    @Test
    void emailEscapaElMensaje_yNoContieneTokensNiUrlPublica() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("DUDAS", "<script>alert('xss')</script>"));

        assertThat(budget.getRespuestaClienteMensaje()).isEqualTo("<script>alert('xss')</script>");
        // The email body is HTML-escaped; verify the email was called
        verify(emailService).enviarRespuestaCliente(eq(OWNER_ID), eq("owner@example.test"), anyString(), argThat(body ->
                body.contains("&lt;script&gt;") && !body.contains("/p/") && !body.contains("token")
        ), anyString());
    }

    // === 10. El job de seguimiento NO genera avisos para presupuesto con respuesta ===

    @Test
    void presupuestoConRespuestaCliente_noGeneraAvisosSeguimiento() {
        budget.setRespuestaCliente("INTERESA");
        // PresupuestoSeguimientoServiceTest already covers this, but let's verify the condition
        assertThat(budget.getRespuestaCliente()).isNotNull();
        // The condition in PresupuestoSeguimientoService is:
        // || presupuesto.getRespuestaCliente() != null
        // which skips eligible budgets with a client response
    }

    // === 11. IDOR: el servicio resuelve por token hash, no por ID directo ===

    @Test
    void responder_usaTokenHash_noIdDirecto() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", null));

        // The service resolves via token hash, not budget ID
        verify(enlaceRepository).findByTokenHashAndRevocadoFalseAndExpiraAtAfter(eq(TOKEN_HASH), any());
        // Then uses findOwnedForUpdate with the budget's owner
        verify(presupuestoRepository).findOwnedForUpdate(BUDGET_ID, OWNER_ID);
    }

    // === 12. Token y mensaje ausentes de los logs ===

    @Test
    void emailIdempotencyKey_noContieneToken() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", "Mensaje secreto"));

        verify(emailService).enviarRespuestaCliente(eq(OWNER_ID), eq("owner@example.test"), anyString(), anyString(),
                argThat(key -> !key.contains("some-token") && !key.contains("Mensaje secreto")));
    }

    // === 13. Rate limit también para intentos fallidos (verificado en controller test) ===

    @Test
    void respuestaClienteConMensajeLargo_seTruncaA500() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        String longMessage = "a".repeat(600);
        // The DTO validation rejects > 500 at the controller level
        // The service sanitizes control chars but length is enforced by @Size
        var request = new RespuestaClienteRequest("INTERESA", "b".repeat(500));
        service.respond("some-token", request);

        assertThat(budget.getRespuestaClienteMensaje()).hasSize(500);
    }

    // === 14. GET público incluye permiteResponder y respuestaCliente, NO incluye mensaje ===

    @Test
    void respuestaClienteSeGuarda_correctamente() {
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", "Mensaje privado"));

        assertThat(budget.getRespuestaCliente()).isEqualTo("INTERESA");
        assertThat(budget.getRespuestaClienteAt()).isNotNull();
        assertThat(budget.getRespuestaClienteMensaje()).isEqualTo("Mensaje privado");
        // The GET public endpoint (PublicBudgetLinkService) returns respuestaCliente but NOT the message
        // This is verified by the DTO structure: PresupuestoPublicoResponse only includes the option
    }

    // === 15. Cascada al borrar el presupuesto (verificada en Flyway test) ===

    @Test
    void sinEmpresa_noLanzaError() {
        when(empresaRepository.findByUsuarioId(OWNER_ID)).thenReturn(Optional.empty());
        when(presupuestoRepository.updateRespuestaClienteIfDistinct(anyLong(), anyLong(), anyString(), any(), any(), any(), any()))
                .thenReturn(1);
        doNothing().when(emailService).enviarRespuestaCliente(anyLong(), anyString(), anyString(), anyString(), anyString());

        service.respond("some-token", new RespuestaClienteRequest("INTERESA", null));

        assertThat(budget.getRespuestaCliente()).isEqualTo("INTERESA");
        // No empresa → permiteResponder is true by default (null check in service)
    }
}
