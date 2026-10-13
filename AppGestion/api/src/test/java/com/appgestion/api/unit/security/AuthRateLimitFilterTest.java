package com.appgestion.api.unit.security;

import com.appgestion.api.security.AuthRateLimitFilter;
import com.appgestion.api.util.TrustedClientAddressResolver;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** SEC-11: cobertura de endpoints sensibles y estado acotado por IP. */
class AuthRateLimitFilterTest {

    private final FilterChain chain = mock(FilterChain.class);

    private AuthRateLimitFilter filter(int capacity) {
        return new AuthRateLimitFilter(true, capacity, 15, TrustedClientAddressResolver.ClientIpMode.NONE, List.of());
    }

    private int call(AuthRateLimitFilter f, String method, String uri, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, chain);
        return res.getStatus();
    }

    @Test
    void login_superadoElCupo_devuelve429() throws Exception {
        AuthRateLimitFilter f = filter(2);
        assertThat(call(f, "POST", "/auth/login", "1.1.1.1")).isEqualTo(200);
        assertThat(call(f, "POST", "/auth/login", "1.1.1.1")).isEqualTo(200);
        assertThat(call(f, "POST", "/auth/login", "1.1.1.1")).isEqualTo(429);
    }

    @Test
    void elCupoEsPorIp() throws Exception {
        AuthRateLimitFilter f = filter(1);
        assertThat(call(f, "POST", "/auth/login", "1.1.1.1")).isEqualTo(200);
        assertThat(call(f, "POST", "/auth/login", "1.1.1.1")).isEqualTo(429);
        assertThat(call(f, "POST", "/auth/login", "2.2.2.2")).isEqualTo(200);
    }

    @Test
    void inviteVerify_porGet_estaLimitado() throws Exception {
        AuthRateLimitFilter f = filter(1);
        assertThat(call(f, "GET", "/auth/invite/verify", "1.1.1.1")).isEqualTo(200);
        assertThat(call(f, "GET", "/auth/invite/verify", "1.1.1.1")).isEqualTo(429);
    }

    @Test
    void endpointsAutenticadosQueComprueban_credenciales_estanLimitados() throws Exception {
        for (String uri : List.of("/auth/change-password", "/auth/totp/disable", "/auth/totp/setup/confirm")) {
            AuthRateLimitFilter f = filter(1);
            assertThat(call(f, "POST", uri, "1.1.1.1")).as(uri).isEqualTo(200);
            assertThat(call(f, "POST", uri, "1.1.1.1")).as(uri).isEqualTo(429);
        }
    }

    @Test
    void otrasRutas_noSeLimitan() throws Exception {
        AuthRateLimitFilter f = filter(1);
        for (int i = 0; i < 5; i++) {
            assertThat(call(f, "GET", "/auth/me", "1.1.1.1")).isEqualTo(200);
        }
    }
}
