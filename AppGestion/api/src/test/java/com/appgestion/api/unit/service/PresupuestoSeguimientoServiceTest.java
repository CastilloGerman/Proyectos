package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.*;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.dto.request.SeguimientoPresupuestosPatchRequest;
import com.appgestion.api.repository.*;
import com.appgestion.api.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresupuestoSeguimientoServiceTest {
    private static final Long OWNER_ID = 8L;
    private static final Long BUDGET_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    @Mock EmpresaRepository empresaRepository;
    @Mock PresupuestoRepository presupuestoRepository;
    @Mock PresupuestoEnlaceRepository enlaceRepository;
    @Mock PresupuestoSeguimientoAvisoRepository avisoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock NotificacionService notificacionService;
    @Mock EmailService emailService;

    private Usuario owner;
    private Empresa settings;
    private Presupuesto budget;
    private PresupuestoSeguimientoService service;

    @BeforeEach
    void setUp() {
        owner = new Usuario();
        owner.setId(OWNER_ID);
        owner.setNombre("Owner");
        owner.setEmail("owner@example.test");
        owner.setUiLocale("es");
        owner.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        owner.setActivo(true);

        settings = new Empresa();
        settings.setUsuario(owner);
        budget = new Presupuesto();
        budget.setId(BUDGET_ID);
        budget.setUsuario(owner);
        budget.setCliente(new Cliente());
        budget.getCliente().setNombre("Cliente");
        budget.setEstado("Pendiente");
        budget.setEnviadoAt(LocalDateTime.of(2026, 10, 6, 10, 0));

        service = createService(-1);
        lenient().when(usuarioRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        lenient().when(empresaRepository.findByUsuarioId(OWNER_ID)).thenReturn(Optional.of(settings));
        lenient().when(presupuestoRepository
                .findByUsuarioIdAndEnviadoAtIsNotNullAndSeguimientoSilenciadoFalseAndIdGreaterThanOrderByIdAsc(
                        eq(OWNER_ID), eq(0L),
                        org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(List.of(budget));
        lenient().when(presupuestoRepository.findOwnedForUpdate(BUDGET_ID, OWNER_ID)).thenReturn(Optional.of(budget));
        lenient().when(enlaceRepository.latestViewAt(BUDGET_ID)).thenReturn(null);
        lenient().when(avisoRepository.countByPresupuestoId(BUDGET_ID)).thenReturn(0L);
        lenient().when(avisoRepository.findTopByPresupuestoIdOrderByCreadoAtDesc(BUDGET_ID)).thenReturn(Optional.empty());
    }

    @Test
    void createsNoOpenAlertAtThreeCalendarDaysAndNotifiesOnlyAfterInsert() {
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 1, NOW)).thenReturn(1);

        assertThat(service.processOwner(OWNER_ID, null)).isEqualTo(1);

        verify(notificacionService).presupuestoSeguimiento(owner, "Cliente", BUDGET_ID, "NO_ABIERTO", 3);
    }

    @Test
    void ownerWithoutCompanyConfigurationUsesDefaultPreferences() {
        when(empresaRepository.findByUsuarioId(OWNER_ID)).thenReturn(Optional.empty());
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 1, NOW)).thenReturn(1);

        assertThat(service.processOwner(OWNER_ID, null)).isEqualTo(1);

        verify(notificacionService).presupuestoSeguimiento(owner, "Cliente", BUDGET_ID, "NO_ABIERTO", 3);
        verifyNoInteractions(emailService);
    }

    @Test
    void doesNotAlertBeforeThreeDays() {
        budget.setEnviadoAt(LocalDateTime.of(2026, 10, 7, 10, 0));

        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        verify(avisoRepository, never()).insertIfAbsent(anyLong(), anyString(), anyInt(), any());
        verifyNoInteractions(notificacionService, emailService);
    }

    @Test
    void usesLatestViewRatherThanSendDateForOpenedBudget() {
        budget.setEnviadoAt(LocalDateTime.of(2026, 10, 1, 10, 0));
        when(enlaceRepository.latestViewAt(BUDGET_ID)).thenReturn(Instant.parse("2026-10-06T12:00:00Z"));
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "ABIERTO_SIN_RESPUESTA", 1, NOW)).thenReturn(1);

        assertThat(service.processOwner(OWNER_ID, null)).isEqualTo(1);

        verify(notificacionService).presupuestoSeguimiento(owner, "Cliente", BUDGET_ID,
                "ABIERTO_SIN_RESPUESTA", 3);
    }

    @Test
    void openedBudgetDoesNotQualifyUntilThreeDaysAfterItsLatestViewEvenIfSentEarlier() {
        budget.setEnviadoAt(LocalDateTime.of(2026, 9, 1, 10, 0));
        when(enlaceRepository.latestViewAt(BUDGET_ID)).thenReturn(Instant.parse("2026-10-08T12:00:00Z"));

        assertThat(service.processOwner(OWNER_ID, null)).isZero();

        verify(avisoRepository, never()).insertIfAbsent(anyLong(), anyString(), anyInt(), any());
        verifyNoInteractions(notificacionService, emailService);
    }

    @Test
    void skipsFinalStatesAndSilencedBudgets() {
        for (String state : List.of("Aceptado", "Rechazado", "En ejecución", "En ejecucion", "Desconocido")) {
            budget.setEstado(state);
            assertThat(service.processOwner(OWNER_ID, null)).isZero();
        }
        budget.setEstado("Pendiente");
        budget.setSeguimientoSilenciado(true);
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        verify(avisoRepository, never()).insertIfAbsent(anyLong(), anyString(), anyInt(), any());
    }

    @Test
    void excludesDisabledSettingExpiredOrDeletedOwners() {
        settings.setSeguimientoActivo(false);
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        settings.setSeguimientoActivo(true);
        owner.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED);
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        owner.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        owner.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE);
        owner.setTrialEndDate(java.time.LocalDate.of(2026, 10, 8));
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        owner.setTrialEndDate(null);
        owner.setSubscriptionStatus(SubscriptionStatus.TRIALING);
        owner.setSubscriptionCurrentPeriodEnd(LocalDateTime.of(2026, 10, 8, 12, 0));
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        owner.setSubscriptionCurrentPeriodEnd(null);
        owner.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        owner.setActivo(false);
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        owner.setActivo(true);
        when(usuarioRepository.findById(OWNER_ID)).thenReturn(Optional.empty());
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        verify(presupuestoRepository, never())
                .findByUsuarioIdAndEnviadoAtIsNotNullAndSeguimientoSilenciadoFalseAndIdGreaterThanOrderByIdAsc(
                        anyLong(), anyLong(), any());
    }

    @Test
    void enforcesPerBudgetMaximumAndMinimumCalendarDaySpacing() {
        settings.setSeguimientoMaxAvisos(1);
        when(avisoRepository.countByPresupuestoId(BUDGET_ID)).thenReturn(1L);
        assertThat(service.processOwner(OWNER_ID, null)).isZero();

        settings.setSeguimientoMaxAvisos(2);
        when(avisoRepository.countByPresupuestoId(BUDGET_ID)).thenReturn(1L);
        PresupuestoSeguimientoAviso previous = new PresupuestoSeguimientoAviso();
        previous.setCreadoAt(NOW.minusSeconds(24 * 60 * 60));
        when(avisoRepository.findTopByPresupuestoIdOrderByCreadoAtDesc(BUDGET_ID))
                .thenReturn(Optional.of(previous));
        assertThat(service.processOwner(OWNER_ID, null)).isZero();

        previous.setCreadoAt(Instant.parse("2026-10-06T12:00:00Z"));
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 2, NOW)).thenReturn(1);
        assertThat(service.processOwner(OWNER_ID, null)).isEqualTo(1);
    }

    @Test
    void duplicateInsertAndSameDayRerunNeverCreateAnotherNotification() {
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 1, NOW)).thenReturn(0);
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        verify(notificacionService, never()).presupuestoSeguimiento(any(), anyString(), anyLong(), anyString(), anyLong());

        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 1, NOW)).thenReturn(1);
        assertThat(service.processOwner(OWNER_ID, null)).isEqualTo(1);
        PresupuestoSeguimientoAviso created = new PresupuestoSeguimientoAviso();
        created.setCreadoAt(NOW);
        when(avisoRepository.countByPresupuestoId(BUDGET_ID)).thenReturn(1L);
        when(avisoRepository.findTopByPresupuestoIdOrderByCreadoAtDesc(BUDGET_ID))
                .thenReturn(Optional.of(created));
        assertThat(service.processOwner(OWNER_ID, null)).isZero();
        verify(notificacionService, times(1))
                .presupuestoSeguimiento(owner, "Cliente", BUDGET_ID, "NO_ABIERTO", 3);
    }

    @Test
    void enqueuesOnePrivateDigestOnlyForNewAlertsWhenEnabled() {
        settings.setSeguimientoEmailResumen(true);
        Presupuesto secondBudget = new Presupuesto();
        secondBudget.setId(43L);
        secondBudget.setUsuario(owner);
        secondBudget.setCliente(new Cliente());
        secondBudget.getCliente().setNombre("Segundo cliente");
        secondBudget.setEstado("Pendiente");
        secondBudget.setEnviadoAt(LocalDateTime.of(2026, 10, 6, 10, 0));
        when(presupuestoRepository
                .findByUsuarioIdAndEnviadoAtIsNotNullAndSeguimientoSilenciadoFalseAndIdGreaterThanOrderByIdAsc(
                        eq(OWNER_ID), eq(0L), any())).thenReturn(List.of(budget, secondBudget));
        when(presupuestoRepository.findOwnedForUpdate(43L, OWNER_ID)).thenReturn(Optional.of(secondBudget));
        when(enlaceRepository.latestViewAt(43L)).thenReturn(null);
        when(avisoRepository.countByPresupuestoId(43L)).thenReturn(0L);
        when(avisoRepository.findTopByPresupuestoIdOrderByCreadoAtDesc(43L)).thenReturn(Optional.empty());
        when(avisoRepository.insertIfAbsent(anyLong(), anyString(), anyInt(), any())).thenReturn(1);

        assertThat(service.processOwner(OWNER_ID, null)).isEqualTo(2);

        verify(emailService).enviarResumenSeguimiento(eq(OWNER_ID), eq("owner@example.test"),
                contains("seguimiento"), argThat(body -> body.contains("Cliente")
                        && body.contains("Segundo cliente") && body.contains("42") && body.contains("43")
                        && !body.contains("NO")
                        && !body.contains("/p/") && !body.contains("token")),
                eq("presupuesto-seguimiento-8-2026-10-09"));
    }

    @Test
    void doesNotQueueDigestWhenEmailSummaryIsDisabled() {
        settings.setSeguimientoEmailResumen(false);
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 1, NOW)).thenReturn(1);
        service.processOwner(OWNER_ID, null);
        verifyNoInteractions(emailService);
    }

    @Test
    void doesNotQueueDigestWhenThereAreNoNewAlerts() {
        settings.setSeguimientoEmailResumen(true);
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 1, NOW)).thenReturn(0);

        assertThat(service.processOwner(OWNER_ID, null)).isZero();

        verifyNoInteractions(emailService);
    }

    @Test
    void localizesDigestSubjectAndBodyForAllSupportedUserLanguages() {
        settings.setSeguimientoEmailResumen(true);
        String[][] cases = {
                {"es-ES", "Resumen diario de seguimiento de presupuestos", "Presupuestos que necesitan seguimiento"},
                {"en-GB", "Daily budget follow-up summary", "Budgets that need follow-up"},
                {"fr-FR", "Résumé quotidien du suivi des devis", "Devis nécessitant un suivi"},
                {"ro-RO", "Rezumat zilnic pentru urmărirea ofertelor", "Oferte care necesită urmărire"},
                {"uk-UA", "Щоденний підсумок нагадувань про кошториси", "Кошториси, які потребують уваги"},
        };
        when(avisoRepository.insertIfAbsent(anyLong(), anyString(), anyInt(), any())).thenReturn(1);

        for (String[] languageCase : cases) {
            owner.setUiLocale(languageCase[0]);
            service.processOwner(OWNER_ID, null);
            var subject = org.mockito.ArgumentCaptor.forClass(String.class);
            var body = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(emailService, atLeastOnce()).enviarResumenSeguimiento(
                    eq(OWNER_ID), eq("owner@example.test"), subject.capture(), body.capture(), anyString());
            assertThat(subject.getValue()).isEqualTo(languageCase[1]);
            assertThat(body.getValue()).contains(languageCase[2], "Cliente", "/presupuestos")
                    .doesNotContain("/p/", "token");
            clearInvocations(emailService);
        }
    }

    @Test
    void concurrentWorkersOnlyNotifyWhenAtomicInsertWins() throws Exception {
        CountDownLatch bothReachedInsert = new CountDownLatch(2);
        AtomicInteger winners = new AtomicInteger();
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 1, NOW)).thenAnswer(invocation -> {
            bothReachedInsert.countDown();
            assertThat(bothReachedInsert.await(5, TimeUnit.SECONDS)).isTrue();
            return winners.getAndIncrement() == 0 ? 1 : 0;
        });

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.processOwner(OWNER_ID, null));
            var second = executor.submit(() -> service.processOwner(OWNER_ID, null));
            assertThat(first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS)).isEqualTo(1);
        }
        verify(notificacionService, times(1))
                .presupuestoSeguimiento(owner, "Cliente", BUDGET_ID, "NO_ABIERTO", 3);
    }

    @Test
    void settingsAndSilencingRemainOwnerScopedAndLocalZeroIsExplicit() {
        when(presupuestoRepository.findOwnedForUpdate(BUDGET_ID, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setSilenced(BUDGET_ID, 99L, true))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.setSilenced(BUDGET_ID, 99L, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(presupuestoRepository, times(2)).findOwnedForUpdate(BUDGET_ID, 99L);

        when(empresaRepository.findByUsuarioId(OWNER_ID)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(OWNER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.patchSettings(OWNER_ID,
                new SeguimientoPresupuestosPatchRequest(true, 0, 2, false)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.patchSettings(OWNER_ID,
                new SeguimientoPresupuestosPatchRequest(true, -1, 2, false)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        when(usuarioRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(empresaRepository.findByUsuarioId(OWNER_ID)).thenReturn(Optional.of(settings));
        assertThatThrownBy(() -> service.processOwner(OWNER_ID, 0))
                .isInstanceOf(IllegalArgumentException.class);
        PresupuestoSeguimientoService local = createService(0);
        when(avisoRepository.insertIfAbsent(BUDGET_ID, "NO_ABIERTO", 1, NOW)).thenReturn(1);
        assertThat(local.processOwner(OWNER_ID, 0)).isEqualTo(1);
    }

    @Test
    void settingsAreLoadedAndPatchedOnlyForTheAuthenticatedOwnerId() {
        when(empresaRepository.findByUsuarioId(99L)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(owner));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var settings = service.patchSettings(99L,
                new SeguimientoPresupuestosPatchRequest(false, 30, 5, true));

        assertThat(settings.seguimientoActivo()).isFalse();
        assertThat(settings.seguimientoDiasEspera()).isEqualTo(30);
        assertThat(settings.seguimientoMaxAvisos()).isEqualTo(5);
        assertThat(settings.seguimientoEmailResumen()).isTrue();
        verify(empresaRepository).findByUsuarioId(99L);
        verify(empresaRepository, never()).findByUsuarioId(OWNER_ID);
    }

    private PresupuestoSeguimientoService createService(int localDaysOverride) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(localDaysOverride == 0 ? "local" : "test");
        return new PresupuestoSeguimientoService(
                empresaRepository, presupuestoRepository, enlaceRepository, avisoRepository, usuarioRepository,
                notificacionService, emailService, Clock.fixed(NOW, ZoneOffset.UTC),
                environment, "https://app.example.test", localDaysOverride);
    }
}
