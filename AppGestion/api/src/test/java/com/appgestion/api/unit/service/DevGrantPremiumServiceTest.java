package com.appgestion.api.unit.service;

import com.appgestion.api.config.AppDevProperties;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.service.DevGrantPremiumService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevGrantPremiumServiceTest {

    @Test
    void requireCanGrant_conAllowlistVaciaDeniegaAunqueEsteHabilitado() {
        AppDevProperties properties = new AppDevProperties();
        properties.setGrantPremiumEnabled(true);
        properties.setGrantPremiumEmailAllowlist("");
        DevGrantPremiumService service = new DevGrantPremiumService(properties);

        assertThat(service.isAvailableForUi(usuario("user@test.local"))).isFalse();
        assertThatThrownBy(() -> service.requireCanGrant(usuario("user@test.local")))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void requireCanGrant_conEmailEnAllowlistPermiteSinDistinguirMayusculas() {
        AppDevProperties properties = new AppDevProperties();
        properties.setGrantPremiumEnabled(true);
        properties.setGrantPremiumEmailAllowlist("Allowed@Test.Local");
        DevGrantPremiumService service = new DevGrantPremiumService(properties);

        Usuario usuario = usuario("allowed@test.local");

        assertThat(service.isAvailableForUi(usuario)).isTrue();
        service.requireCanGrant(usuario);
    }

    @Test
    void requireCanGrant_conFuncionDeshabilitadaDevuelveNotFound() {
        AppDevProperties properties = new AppDevProperties();
        properties.setGrantPremiumEnabled(false);
        properties.setGrantPremiumEmailAllowlist("user@test.local");
        DevGrantPremiumService service = new DevGrantPremiumService(properties);

        assertThatThrownBy(() -> service.requireCanGrant(usuario("user@test.local")))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        return usuario;
    }
}
