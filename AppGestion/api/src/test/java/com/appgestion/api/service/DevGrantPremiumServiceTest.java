package com.appgestion.api.service;

import com.appgestion.api.config.AppDevProperties;
import com.appgestion.api.domain.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevGrantPremiumServiceTest {

    @Test
    void enabledGrantWithoutAllowlistDeniesAuthenticatedUsers() {
        AppDevProperties properties = new AppDevProperties();
        properties.setGrantPremiumEnabled(true);
        properties.setGrantPremiumEmailAllowlist("");
        DevGrantPremiumService service = new DevGrantPremiumService(properties);

        Usuario usuario = usuario("user@example.com");

        assertThat(service.isAvailableForUi(usuario)).isFalse();
        assertThatThrownBy(() -> service.requireCanGrant(usuario))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void enabledGrantAllowsOnlyExplicitAllowlistEmailsCaseInsensitively() {
        AppDevProperties properties = new AppDevProperties();
        properties.setGrantPremiumEnabled(true);
        properties.setGrantPremiumEmailAllowlist("allowed@example.com, Second@Test.Local");
        DevGrantPremiumService service = new DevGrantPremiumService(properties);

        assertThat(service.isAvailableForUi(usuario("ALLOWED@example.com"))).isTrue();
        assertThat(service.isAvailableForUi(usuario("second@test.local"))).isTrue();
        assertThat(service.isAvailableForUi(usuario("other@example.com"))).isFalse();
    }

    private static Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        return usuario;
    }
}
