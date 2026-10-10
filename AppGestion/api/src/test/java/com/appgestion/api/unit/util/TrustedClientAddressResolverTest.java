package com.appgestion.api.unit.util;

import com.appgestion.api.util.TrustedClientAddressResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrustedClientAddressResolverTest {

    // ── Modo NONE ──

    @Test
    @DisplayName("[none] Debería devolver siempre la IP peer, ignorando cabeceras")
    void none_shouldReturnPeerIpIgnoringHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(request.getHeader("x-vercel-forwarded-for")).thenReturn("1.2.3.4");

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.NONE, List.of());

        assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("[none] Ataque: cabecera falsificada se ignora en modo none")
    void none_attackFakeHeaderIgnored() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.666");
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(request.getHeader("x-vercel-forwarded-for")).thenReturn("127.0.0.1");

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.NONE, List.of());

        assertEquals("203.0.113.666", ip);
    }

    // ── Modo TRUSTED_PROXIES ──

    @Test
    @DisplayName("[trusted-proxies] Debería devolver la IP peer cuando no hay proxies de confianza")
    void trusted_shouldReturnPeerIpWhenNoTrustedProxies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.TRUSTED_PROXIES, List.of());

        assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("[trusted-proxies] Debería ignorar X-Forwarded-For cuando el peer NO es confiable")
    void trusted_shouldIgnoreXForwardedForWhenPeerNotTrusted() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 203.0.113.50");

        List<TrustedClientAddressResolver.Cidr> trusted = List.of(
                TrustedClientAddressResolver.Cidr.parse("127.0.0.1/8")
        );

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.TRUSTED_PROXIES, trusted);

        assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("[trusted-proxies] Debería leer X-Forwarded-For cuando el peer SÍ es confiable")
    void trusted_shouldReadXForwardedForWhenPeerIsTrusted() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50, 70.40.201.1");

        List<TrustedClientAddressResolver.Cidr> trusted = List.of(
                TrustedClientAddressResolver.Cidr.parse("10.0.0.0/8")
        );

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.TRUSTED_PROXIES, trusted);

        assertEquals("203.0.113.50", ip);
    }

    @Test
    @DisplayName("[trusted-proxies] Ataque: cliente falsifica X-Forwarded-For desde peer no confiable")
    void trusted_attackFakeXForwardedForFromUntrustedPeer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.666");
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");

        List<TrustedClientAddressResolver.Cidr> trusted = List.of(
                TrustedClientAddressResolver.Cidr.parse("104.18.0.0/15")
        );

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.TRUSTED_PROXIES, trusted);

        assertEquals("203.0.113.666", ip);
    }

    // ── Modo VERCEL ──

    @Test
    @DisplayName("[vercel] Debería usar x-vercel-forwarded-for cuando está presente")
    void vercel_shouldUseVercelForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-vercel-forwarded-for")).thenReturn("203.0.113.99");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.VERCEL, List.of());

        assertEquals("203.0.113.99", ip);
    }

    @Test
    @DisplayName("[vercel] Debería fallback a X-Forwarded-For si falta x-vercel-forwarded-for")
    void vercel_shouldFallbackToXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-vercel-forwarded-for")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1, 192.0.2.1");

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.VERCEL, List.of());

        // Última entrada de X-Forwarded-For (Vercel la sobrescribe)
        assertEquals("192.0.2.1", ip);
    }

    @Test
    @DisplayName("[vercel] Debería devolver peer si faltan ambas cabeceras")
    void vercel_shouldReturnPeerWhenNoHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-vercel-forwarded-for")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.VERCEL, List.of());

        assertEquals("10.0.0.1", ip);
    }

    @Test
    @DisplayName("[vercel] Debería usar x-vercel-forwarded-for aunque haya otro proxy delante")
    void vercel_shouldPreferVercelHeaderOverXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("x-vercel-forwarded-for")).thenReturn("203.0.113.99");
        // Un proxy delante podría haber añadido entradas a X-Forwarded-For
        when(request.getHeader("X-Forwarded-For")).thenReturn("93.184.216.34, 203.0.113.99");

        String ip = TrustedClientAddressResolver.resolveStatic(
                request, TrustedClientAddressResolver.ClientIpMode.VERCEL, List.of());

        // x-vercel-forwarded-for sobrevive; se usa el primer valor
        assertEquals("203.0.113.99", ip);
    }

    // ── CIDR utilities ──

    @Test
    @DisplayName("Cidr.parseList debería devolver lista vacía para string vacío")
    void parseListShouldReturnEmptyForBlankString() {
        List<TrustedClientAddressResolver.Cidr> list = TrustedClientAddressResolver.Cidr.parseList("");
        assertEquals(0, list.size());

        list = TrustedClientAddressResolver.Cidr.parseList(null);
        assertEquals(0, list.size());

        list = TrustedClientAddressResolver.Cidr.parseList("  ");
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Cidr.parseList debería parsear múltiples CIDR")
    void parseListShouldParseMultipleCidrs() {
        List<TrustedClientAddressResolver.Cidr> list = TrustedClientAddressResolver.Cidr.parseList(
                "10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16"
        );
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Cidr.contains debería funcionar con rangos parciales")
    void cidrContainsShouldWorkWithPartialRanges() {
        TrustedClientAddressResolver.Cidr cidr = TrustedClientAddressResolver.Cidr.parse("192.168.1.0/24");

        assertEquals(true, cidr.contains("192.168.1.1"));
        assertEquals(true, cidr.contains("192.168.1.255"));
        assertEquals(false, cidr.contains("192.168.2.1"));
    }
}
