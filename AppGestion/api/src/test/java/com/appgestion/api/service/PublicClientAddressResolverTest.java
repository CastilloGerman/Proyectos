package com.appgestion.api.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicClientAddressResolverTest {
    private final PublicClientAddressResolver resolver =
            new PublicClientAddressResolver("trusted-proxies", "10.0.0.0/8,127.0.0.1/32");

    @Test
    void trustsForwardedAddressOnlyWhenImmediatePeerIsConfiguredProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.2.3.4");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7, 10.2.3.3");
        // En modo trusted-proxies, el peer 10.2.3.4 está en 10.0.0.0/8 → se lee X-Forwarded-For (primer valor)
        assertEquals("198.51.100.7", resolver.resolve(request));
    }

    @Test
    void vercelMode_usesVercelForwardedFor() {
        PublicClientAddressResolver vercelResolver = new PublicClientAddressResolver("vercel", "");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-vercel-forwarded-for")).thenReturn("203.0.113.99");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");
        assertEquals("203.0.113.99", vercelResolver.resolve(request));
    }

    @Test
    void ignoresForwardedAddressFromUntrustedPeer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7");
        assertEquals("203.0.113.9", resolver.resolve(request));
    }

    @Test
    void doesNotTrustForwardedAddressByDefault() {
        PublicClientAddressResolver defaultResolver = new PublicClientAddressResolver("none", "");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7");
        assertEquals("203.0.113.9", defaultResolver.resolve(request));
    }
}
