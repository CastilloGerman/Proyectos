package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Cliente;
import com.appgestion.api.domain.entity.Presupuesto;
import com.appgestion.api.domain.entity.PresupuestoEnlace;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.PresupuestoEnlaceRepository;
import com.appgestion.api.repository.PresupuestoRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicBudgetLinkServiceTest {
    @Test
    void createReturnsUrlSafeTokenButPersistsOnlyItsHash() {
        Presupuesto presupuesto = presupuesto();
        PresupuestoRepository budgets = mock(PresupuestoRepository.class);
        PresupuestoEnlaceRepository links = mock(PresupuestoEnlaceRepository.class);
        when(budgets.findOwnedForUpdate(12L, 4L)).thenReturn(Optional.of(presupuesto));
        when(links.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PublicBudgetLinkService service = service(budgets, links, mock(NotificacionService.class));

        var response = service.create(12L, 4L);

        String token = response.url().substring(response.url().lastIndexOf('/') + 1);
        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        var captor = org.mockito.ArgumentCaptor.forClass(PresupuestoEnlace.class);
        verify(links).save(captor.capture());
        assertEquals(PublicBudgetLinkService.hash(token), captor.getValue().getTokenHash());
        assertFalse(captor.getValue().getTokenHash().equals(token));
        verify(links, never()).revokeAll(anyLong(), anyLong());
    }

    @Test
    void createForAnotherOwnersBudgetReturnsNotFoundAndDoesNotCreateLink() {
        PresupuestoRepository budgets = mock(PresupuestoRepository.class);
        PresupuestoEnlaceRepository links = mock(PresupuestoEnlaceRepository.class);
        when(budgets.findOwnedForUpdate(12L, 99L)).thenReturn(Optional.empty());
        var service = service(budgets, links, mock(NotificacionService.class));

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.create(12L, 99L));

        assertEquals(404, error.getStatusCode().value());
        verifyNoInteractions(links);
    }

    @Test
    void concurrentFirstViewAttemptsProduceExactlyOneNotification() throws Exception {
        Presupuesto presupuesto = presupuesto();
        PresupuestoEnlace link = new PresupuestoEnlace();
        link.setId(8L);
        link.setPresupuesto(presupuesto);
        link.setTokenHash(PublicBudgetLinkService.hash("safe-token"));
        link.setExpiraAt(Instant.now().plusSeconds(300));
        PresupuestoRepository budgets = mock(PresupuestoRepository.class);
        PresupuestoEnlaceRepository links = mock(PresupuestoEnlaceRepository.class);
        NotificacionService notifications = mock(NotificacionService.class);
        when(links.findByTokenHashAndRevocadoFalseAndExpiraAtAfter(eq(link.getTokenHash()), any())).thenReturn(Optional.of(link));
        AtomicInteger winners = new AtomicInteger();
        AtomicInteger notificationWinners = new AtomicInteger();
        when(links.markFirstView(eq(8L), any())).thenAnswer(ignored -> winners.getAndIncrement() == 0 ? 1 : 0);
        when(budgets.markLinkViewNotificationSent(12L)).thenAnswer(ignored -> notificationWinners.getAndIncrement() == 0 ? 1 : 0);
        PublicBudgetLinkService service = service(budgets, links, notifications);

        int calls = 12;
        ExecutorService pool = Executors.newFixedThreadPool(calls);
        CountDownLatch start = new CountDownLatch(1);
        try {
            for (int i = 0; i < calls; i++) {
                pool.submit(() -> {
                    try { start.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    service.registerView("safe-token");
                });
            }
            start.countDown();
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        }
        verify(notifications, times(1)).presupuestoVisto(any(), eq("Ana"), eq(12L));
        verify(links, times(calls - 1)).countSubsequentView(eq(8L), any(), any());
    }

    @Test
    void rateLimiterSeparatelyLimitsIpAndTokenWithOneMinuteWindow() {
        PublicLinkRateLimiter limiter = new PublicLinkRateLimiter(10_000, 2);
        assertTrue(limiter.allow("192.0.2.1", "a"));
        assertTrue(limiter.allow("192.0.2.1", "b"));
        assertTrue(limiter.allow("192.0.2.1", "c"));
        assertTrue(limiter.allow("192.0.2.1", "a"));
        assertFalse(limiter.allow("192.0.2.1", "a"));
    }

    @Test
    void repeatedSendsKeepPriorLinksActiveAndExplicitRegenerationRevokesThem() {
        Presupuesto presupuesto = presupuesto();
        PresupuestoRepository budgets = mock(PresupuestoRepository.class);
        PresupuestoEnlaceRepository links = mock(PresupuestoEnlaceRepository.class);
        when(budgets.findOwnedForUpdate(12L, 4L)).thenReturn(Optional.of(presupuesto));
        when(links.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(budgets, links, mock(NotificacionService.class));

        service.create(12L, 4L);
        service.create(12L, 4L);

        verify(links, times(2)).save(any(PresupuestoEnlace.class));
        verify(links, never()).revokeAll(anyLong(), anyLong());

        service.regenerate(12L, 4L);
        verify(links).revokeAll(12L, 4L);
    }

    @Test
    void manyTokensFromOneProxyAreNotBlockedByTokenPrincipalLimit() {
        PublicLinkRateLimiter limiter = new PublicLinkRateLimiter(10_000, 1);
        assertTrue(limiter.allow("10.0.0.5", "token-a"));
        assertTrue(limiter.allow("10.0.0.5", "token-b"));
        assertTrue(limiter.allow("10.0.0.5", "token-c"));
        assertFalse(limiter.allow("10.0.0.5", "token-a"));
    }

    @Test
    void activeLinkCapReturnsConflictWithoutPersistingAnotherLink() {
        PresupuestoRepository budgets = mock(PresupuestoRepository.class);
        PresupuestoEnlaceRepository links = mock(PresupuestoEnlaceRepository.class);
        when(budgets.findOwnedForUpdate(12L, 4L)).thenReturn(Optional.of(presupuesto()));
        when(links.countByPresupuestoIdAndPresupuestoUsuarioIdAndRevocadoFalseAndExpiraAtAfter(
                eq(12L), eq(4L), any())).thenReturn(10L);
        var service = service(budgets, links, mock(NotificacionService.class));

        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.create(12L, 4L));

        assertEquals(409, error.getStatusCode().value());
        verify(links, never()).save(any());
    }

    private static PublicBudgetLinkService service(PresupuestoRepository budgets, PresupuestoEnlaceRepository links,
                                                    NotificacionService notifications) {
        return new PublicBudgetLinkService(budgets, links, mock(PresupuestoPdfService.class),
                mock(EmpresaService.class), mock(PresupuestoCondicionesService.class),
                notifications, 60, 10, "https://example.test");
    }

    private static Presupuesto presupuesto() {
        Usuario user = new Usuario();
        user.setId(4L);
        user.setNombre("Contratista");
        user.setUiLocale("es");
        Cliente client = new Cliente();
        client.setNombre("Ana");
        Presupuesto budget = new Presupuesto();
        budget.setId(12L);
        budget.setUsuario(user);
        budget.setCliente(client);
        return budget;
    }
}
