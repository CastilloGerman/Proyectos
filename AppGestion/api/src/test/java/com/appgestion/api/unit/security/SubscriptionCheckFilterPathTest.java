package com.appgestion.api.unit.security;

import com.appgestion.api.security.SubscriptionCheckFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** SEC-29: las exclusiones del filtro de suscripcion se comparan por prefijo/patron anclado, no por contains. */
class SubscriptionCheckFilterPathTest {

    @Test
    void rutasLegitimasExcluidas_seSiguenExcluyendo() {
        assertTrue(SubscriptionCheckFilter.isExcludedPath("/auth/profile"));
        assertTrue(SubscriptionCheckFilter.isExcludedPath("/auth/change-password"));
        assertTrue(SubscriptionCheckFilter.isExcludedPath("/subscription/checkout"));
        assertTrue(SubscriptionCheckFilter.isExcludedPath("/webhook/stripe"));
        assertTrue(SubscriptionCheckFilter.isExcludedPath("/publico/presupuestos/abc/responder"));
        assertTrue(SubscriptionCheckFilter.isExcludedPath("/presupuestos/12/enviar-email"));
        assertTrue(SubscriptionCheckFilter.isExcludedPath("/facturas/7/enviar-email"));
    }

    @Test
    void fragmentoExcluidoEnOtraPosicion_noSeSaltaLaComprobacion() {
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/clientes/auth/profile"));
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/facturas/webhook/x"));
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/materiales/dev/1"));
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/presupuestos/enviar-email"));
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/presupuestos/abc/enviar-email"));
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/facturas/1/enviar-email/../../x"));
    }

    @Test
    void rutasDeEscrituraNormales_noEstanExcluidas() {
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/facturas"));
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/clientes/5"));
        assertFalse(SubscriptionCheckFilter.isExcludedPath("/auth/invite"));
    }

    @Test
    void revocarEnlace_soloConDeleteYRutaExacta() {
        assertTrue(SubscriptionCheckFilter.isRevocarEnlace("DELETE", "/presupuestos/5/enlace"));
        assertFalse(SubscriptionCheckFilter.isRevocarEnlace("POST", "/presupuestos/5/enlace"));
        assertFalse(SubscriptionCheckFilter.isRevocarEnlace("DELETE", "/clientes/presupuestos/5/enlace"));
    }

    @Test
    void normalizedPath_quitaElContextPath() {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/api/auth/profile");
        r.setContextPath("/api");
        assertEquals("/auth/profile", SubscriptionCheckFilter.normalizedPath(r));
    }
}
