package com.appgestion.api.integration.multitenancy;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Autentica MockMvc como {@link MultitenancyIntegrationTestSupport#EMAIL_USUARIO_A} sin depender
 * del filtro JWT (misma base de datos que la semilla del test).
 */
public final class MultitenancyAuth {

    private MultitenancyAuth() {
    }

    public static RequestPostProcessor asUsuarioA(UserDetailsService userDetailsService) {
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(
                    MultitenancyIntegrationTestSupport.EMAIL_USUARIO_A);
            return authentication(new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities()));
        } catch (UsernameNotFoundException e) {
            throw new IllegalStateException("Usuario A debe existir en BD de test", e);
        }
    }
}
