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
    void requireCanGrant_enabledButEmptyAllowlist_deniesAccess() {
        DevGrantPremiumService service = service(true, " ");

        assertThat(service.isAvailableForUi(user("user@test.local"))).isFalse();
        assertThatThrownBy(() -> service.requireCanGrant(user("user@test.local")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value())
                        .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void requireCanGrant_enabledAndEmailAllowlisted_allowsAccessIgnoringCase() {
        DevGrantPremiumService service = service(true, "allowed@test.local, other@test.local");

        assertThat(service.isAvailableForUi(user("ALLOWED@test.local"))).isTrue();
        assertThatCode(() -> service.requireCanGrant(user("ALLOWED@test.local"))).doesNotThrowAnyException();
    }

    @Test
    void requireCanGrant_disabled_returnsNotFound() {
        DevGrantPremiumService service = service(false, "allowed@test.local");

        assertThat(service.isAvailableForUi(user("allowed@test.local"))).isFalse();
        assertThatThrownBy(() -> service.requireCanGrant(user("allowed@test.local")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value())
                        .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    private static DevGrantPremiumService service(boolean enabled, String allowlist) {
        AppDevProperties properties = new AppDevProperties();
        properties.setGrantPremiumEnabled(enabled);
        properties.setGrantPremiumEmailAllowlist(allowlist);
        return new DevGrantPremiumService(properties);
    }

    private static Usuario user(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        return usuario;
    }
}
