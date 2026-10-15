package com.appgestion.api.unit.security;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SubscriptionCheckFilter;
import com.appgestion.api.service.SubscriptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionCheckFilterTest {

    private static final String EMAIL = "caducada@ejemplo.test";

    @Mock UsuarioRepository usuarioRepository;
    @Mock SubscriptionService subscriptionService;

    SubscriptionCheckFilter filter;

    @BeforeEach
    void setUp() {
        filter = new SubscriptionCheckFilter(subscriptionService, usuarioRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void suscripcionCaducada_dejaPasarLecturasSinConsultarUsuario() throws Exception {
        for (String method : List.of("GET", "HEAD", "OPTIONS")) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest(method, "/facturas"), response, chain);

            assertThat(chain.getRequest()).as(method).isNotNull();
            assertThat(response.getStatus()).as(method).isNotEqualTo(403);
        }
        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    void suscripcionCaducada_bloqueaEscrituras() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setEmail(EMAIL);
        usuario.setSubscriptionStatus(SubscriptionStatus.TRIAL_EXPIRED);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
        when(subscriptionService.canWrite(usuario)).thenReturn(false);

        for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest(method, "/facturas"), response, chain);

            assertThat(response.getStatus()).as(method).isEqualTo(403);
            assertThat(chain.getRequest()).as(method).isNull();
        }
    }

    @Test
    void rutasExcluidas_pasanAunqueLaSuscripcionEsteCaducada() throws Exception {
        for (String path : List.of(
                "/facturas/7/enviar-email",
                "/presupuestos/12/enviar-email",
                "/auth/profile",
                "/publico/presupuestos/abc")) {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(new MockHttpServletRequest("POST", path), response, chain);

            assertThat(chain.getRequest()).as(path).isNotNull();
            assertThat(response.getStatus()).as(path).isNotEqualTo(403);
        }
        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    void revocarEnlace_pasaConDelete() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("DELETE", "/presupuestos/5/enlace"), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        verify(usuarioRepository, never()).findByEmail(any());
    }
}
