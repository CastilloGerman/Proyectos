package com.appgestion.api.unit.controller;

import com.appgestion.api.controller.PresupuestoPublicoController;
import com.appgestion.api.dto.request.RespuestaClienteRequest;
import com.appgestion.api.dto.response.RespuestaClienteConfirmacionResponse;
import com.appgestion.api.service.PublicBudgetLinkService;
import com.appgestion.api.service.PresupuestoRespuestaClienteService;
import com.appgestion.api.service.PublicLinkRateLimiter;
import com.appgestion.api.service.PublicClientAddressResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PresupuestoPublicoControllerTest {
    @Test
    void missingExpiredAndRevokedLinksHaveSamePublicNotFoundResponse() {
        PublicBudgetLinkService service = mock(PublicBudgetLinkService.class);
        when(service.getPublic("missing")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        when(service.getPublic("expired")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        when(service.getPublic("revoked")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        PresupuestoPublicoController controller = new PresupuestoPublicoController(service,
                mock(PresupuestoRespuestaClienteService.class),
                new PublicLinkRateLimiter(100, 100), new PublicClientAddressResolver("none", "127.0.0.1/32"));

        var missing = controller.get("missing", request());
        var expired = controller.get("expired", request());
        var revoked = controller.get("revoked", request());

        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertEquals(missing.getBody(), expired.getBody());
        assertEquals(missing.getBody(), revoked.getBody());
        assertEquals("no-store", missing.getHeaders().getCacheControl());
        assertEquals("noindex, nofollow", missing.getHeaders().getFirst("X-Robots-Tag"));
        verify(service, never()).registerView(anyString());
    }

    @Test
    void missingExpiredAndRevokedTokensAllCountAgainstTheIpLimit() {
        PublicBudgetLinkService service = mock(PublicBudgetLinkService.class);
        when(service.getPublic(anyString())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        PresupuestoPublicoController controller = new PresupuestoPublicoController(service,
                mock(PresupuestoRespuestaClienteService.class),
                new PublicLinkRateLimiter(3, 100), new PublicClientAddressResolver("none", "127.0.0.1/32"));

        assertEquals(HttpStatus.NOT_FOUND, controller.get("missing", request()).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.get("expired", request()).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.get("revoked", request()).getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, controller.get("another-token", request()).getStatusCode());

        verify(service).getPublic("missing");
        verify(service).getPublic("expired");
        verify(service).getPublic("revoked");
        verify(service, never()).getPublic("another-token");
    }

    @Test
    void postViewedRegistersViewAndRateLimitReturns429() {
        PublicBudgetLinkService service = mock(PublicBudgetLinkService.class);
        PresupuestoPublicoController controller = new PresupuestoPublicoController(service,
                mock(PresupuestoRespuestaClienteService.class),
                new PublicLinkRateLimiter(1, 10), new PublicClientAddressResolver("none", "127.0.0.1/32"));

        var first = controller.viewed("token-a", request());
        var limited = controller.viewed("token-b", request());

        assertEquals(HttpStatus.NO_CONTENT, first.getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, limited.getStatusCode());
        verify(service).registerView("token-a");
        verify(service, never()).registerView("token-b");
    }

    // === Responder endpoint tests ===

    @Test
    void responder_returns200WithConfirmation() {
        PublicBudgetLinkService linkService = mock(PublicBudgetLinkService.class);
        PresupuestoRespuestaClienteService respuestaService = mock(PresupuestoRespuestaClienteService.class);
        PresupuestoPublicoController controller = new PresupuestoPublicoController(linkService, respuestaService,
                new PublicLinkRateLimiter(100, 100), new PublicClientAddressResolver("none", "127.0.0.1/32"));

        var response = controller.respond("token-a", new RespuestaClienteRequest("INTERESA", "Gracias"), request());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof RespuestaClienteConfirmacionResponse);
        assertEquals("no-store", response.getHeaders().getCacheControl());
        verify(respuestaService).respond(eq("token-a"), any(RespuestaClienteRequest.class));
    }

    @Test
    void responder_tokenNotFound_returnsUniform404() {
        PublicBudgetLinkService linkService = mock(PublicBudgetLinkService.class);
        PresupuestoRespuestaClienteService respuestaService = mock(PresupuestoRespuestaClienteService.class);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(respuestaService).respond(eq("bad-token"), any());
        PresupuestoPublicoController controller = new PresupuestoPublicoController(linkService, respuestaService,
                new PublicLinkRateLimiter(100, 100), new PublicClientAddressResolver("none", "127.0.0.1/32"));

        var response = controller.respond("bad-token", new RespuestaClienteRequest("INTERESA", null), request());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("Este enlace no está disponible", body.get("error"));
    }

    @Test
    void responder_cooldownExceeded_returns429WithRetryAfter() {
        PublicBudgetLinkService linkService = mock(PublicBudgetLinkService.class);
        PresupuestoRespuestaClienteService respuestaService = mock(PresupuestoRespuestaClienteService.class);
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Espera antes de cambiar tu aviso"))
                .when(respuestaService).respond(eq("token-a"), any());
        PresupuestoPublicoController controller = new PresupuestoPublicoController(linkService, respuestaService,
                new PublicLinkRateLimiter(100, 100), new PublicClientAddressResolver("none", "127.0.0.1/32"));

        var response = controller.respond("token-a", new RespuestaClienteRequest("DUDAS", null), request());

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("300", response.getHeaders().getFirst("Retry-After"));
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertNotNull(body.get("error"));
    }

    @Test
    void responder_rateLimitOnFailedAttempts_returns429() {
        // Rate limit applies even when the service throws (failed attempt)
        PublicBudgetLinkService linkService = mock(PublicBudgetLinkService.class);
        PresupuestoRespuestaClienteService respuestaService = mock(PresupuestoRespuestaClienteService.class);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND)).when(respuestaService).respond(anyString(), any());
        PresupuestoPublicoController controller = new PresupuestoPublicoController(linkService, respuestaService,
                new PublicLinkRateLimiter(2, 100), new PublicClientAddressResolver("none", "127.0.0.1/32"));

        // First two requests hit the service (both fail with 404)
        assertEquals(HttpStatus.NOT_FOUND, controller.respond("token-a", new RespuestaClienteRequest("INTERESA", null), request()).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.respond("token-a", new RespuestaClienteRequest("DUDAS", null), request()).getStatusCode());
        // Third request is rate-limited before reaching the service
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, controller.respond("token-a", new RespuestaClienteRequest("INTERESA", null), request()).getStatusCode());

        verify(respuestaService, times(2)).respond(anyString(), any());
    }

    @Test
    void responder_securityHeaders_present() {
        PublicBudgetLinkService linkService = mock(PublicBudgetLinkService.class);
        PresupuestoRespuestaClienteService respuestaService = mock(PresupuestoRespuestaClienteService.class);
        PresupuestoPublicoController controller = new PresupuestoPublicoController(linkService, respuestaService,
                new PublicLinkRateLimiter(100, 100), new PublicClientAddressResolver("none", "127.0.0.1/32"));

        var response = controller.respond("token-a", new RespuestaClienteRequest("INTERESA", null), request());

        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("noindex, nofollow", response.getHeaders().getFirst("X-Robots-Tag"));
        assertEquals("no-referrer", response.getHeaders().getFirst("Referrer-Policy"));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
    }

    private static jakarta.servlet.http.HttpServletRequest request() {
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");
        return request;
    }
}
