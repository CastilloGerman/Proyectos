package com.appgestion.api.integration.facturacion;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Autentica MockMvc como {@link FacturaIntegrationTestSupport#EMAIL_USUARIO_FACTURACION} sin JWT.
 */
public final class FacturacionAuth {

    private FacturacionAuth() {
    }

    public static RequestPostProcessor asUsuarioFacturacion(UserDetailsService userDetailsService) {
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(
                    FacturaIntegrationTestSupport.EMAIL_USUARIO_FACTURACION);
            return authentication(new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities()));
        } catch (UsernameNotFoundException e) {
            throw new IllegalStateException("Usuario de facturación debe existir en BD de test", e);
        }
    }
}
