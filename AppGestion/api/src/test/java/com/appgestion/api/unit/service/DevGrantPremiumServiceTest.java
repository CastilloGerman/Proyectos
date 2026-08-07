package com.appgestion.api.unit.service;

import com.appgestion.api.config.AppDevProperties;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.service.DevGrantPremiumService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevGrantPremiumServiceTest {

    @Test
    void requireCanGrant_disabledEndpoint_devuelve404() {
        DevGrantPremiumService service = service(false, "tester@example.com");

        assertThatThrownBy(() -> service.requireCanGrant(usuario("tester@example.com")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void requireCanGrant_enabledWithoutAllowlist_devuelve403() {
        DevGrantPremiumService service = service(true, "");

        assertThatThrownBy(() -> service.requireCanGrant(usuario("tester@example.com")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void requireCanGrant_enabledWithMatchingAllowlist_allowsCaseInsensitiveEmail() {
        DevGrantPremiumService service = service(true, " other@example.com, Tester@Example.com ");

        assertThatCode(() -> service.requireCanGrant(usuario("tester@example.com")))
                .doesNotThrowAnyException();
    }

    @Test
    void isAvailableForUi_requiresEnabledEndpointAndAllowlistedUser() {
        assertThat(service(true, "").isAvailableForUi(usuario("tester@example.com"))).isFalse();
        assertThat(service(true, "tester@example.com").isAvailableForUi(usuario("tester@example.com"))).isTrue();
        assertThat(service(false, "tester@example.com").isAvailableForUi(usuario("tester@example.com"))).isFalse();
    }

    private static DevGrantPremiumService service(boolean enabled, String allowlist) {
        AppDevProperties properties = new AppDevProperties();
        properties.setGrantPremiumEnabled(enabled);
        properties.setGrantPremiumEmailAllowlist(allowlist);
        return new DevGrantPremiumService(properties);
    }

    private static Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        return usuario;
    }
}
