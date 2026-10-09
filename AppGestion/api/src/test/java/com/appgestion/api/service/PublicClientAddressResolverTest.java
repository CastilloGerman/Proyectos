package com.appgestion.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicClientAddressResolverTest {
    private final PublicClientAddressResolver resolver =
            new PublicClientAddressResolver("10.0.0.0/8,127.0.0.1/32");

    @Test
    void trustsForwardedAddressOnlyWhenImmediatePeerIsConfiguredProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.2.3.4");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.2.3.3");
        assertEquals("198.51.100.7", resolver.resolve(request));
    }

    @Test
    void ignoresForwardedAddressFromUntrustedPeer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "198.51.100.7");
        assertEquals("203.0.113.9", resolver.resolve(request));
    }

    @Test
    void doesNotTrustForwardedAddressByDefault() {
        PublicClientAddressResolver defaultResolver = new PublicClientAddressResolver("");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.9");
        request.addHeader("X-Forwarded-For", "198.51.100.7");
        assertEquals("203.0.113.9", defaultResolver.resolve(request));
    }
}
