package com.appgestion.api.integration.presupuesto;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Autentica MockMvc como {@link PresupuestoIntegrationTestSupport#EMAIL_USUARIO_PRESUPUESTOS}.
 */
public final class PresupuestoIntegrationAuth {

    private PresupuestoIntegrationAuth() {
    }

    public static RequestPostProcessor asUsuarioPresupuestos(UserDetailsService userDetailsService) {
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(
                    PresupuestoIntegrationTestSupport.EMAIL_USUARIO_PRESUPUESTOS);
            return authentication(new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities()));
        } catch (UsernameNotFoundException e) {
            throw new IllegalStateException("Usuario presupuestos debe existir en BD de test", e);
        }
    }
}
