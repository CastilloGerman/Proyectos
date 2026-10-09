package com.appgestion.api.unit.controller;

import com.appgestion.api.controller.PresupuestoPublicoController;
import com.appgestion.api.service.PublicBudgetLinkService;
import com.appgestion.api.service.PublicLinkRateLimiter;
import com.appgestion.api.service.PublicClientAddressResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
                new PublicLinkRateLimiter(100, 100), new PublicClientAddressResolver("127.0.0.1/32"));

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
                new PublicLinkRateLimiter(3, 100), new PublicClientAddressResolver("127.0.0.1/32"));

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
                new PublicLinkRateLimiter(1, 10), new PublicClientAddressResolver("127.0.0.1/32"));

        var first = controller.viewed("token-a", request());
        var limited = controller.viewed("token-b", request());

        assertEquals(HttpStatus.NO_CONTENT, first.getStatusCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, limited.getStatusCode());
        verify(service).registerView("token-a");
        verify(service, never()).registerView("token-b");
    }

    private static jakarta.servlet.http.HttpServletRequest request() {
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.20");
        return request;
    }
}
