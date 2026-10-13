package com.appgestion.api.unit.util;

import com.appgestion.api.util.MailFromPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SEC-14: el remitente personalizado solo puede variar el nombre visible, nunca el dominio. */
class MailFromPolicyTest {

    private static final String PLATFORM = "Noemi <noreply@noemiweb.com>";

    @Test
    void aceptaNombrePersonalizadoConDominioDeLaPlataforma() {
        assertThat(MailFromPolicy.validate("Fontanería Ana <noreply@noemiweb.com>", PLATFORM))
                .isEqualTo("Fontanería Ana <noreply@noemiweb.com>");
    }

    @Test
    void aceptaSubdominioYNormalizaElDominio() {
        assertThat(MailFromPolicy.validate("Ana <facturas@Mail.NoemiWeb.com>", PLATFORM))
                .isEqualTo("Ana <facturas@mail.noemiweb.com>");
    }

    @Test
    void aceptaDireccionSimple() {
        assertThat(MailFromPolicy.validate("ana@noemiweb.com", PLATFORM)).isEqualTo("ana@noemiweb.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CEO <ceo@banco.com>",
            "ceo@banco.com",
            "Ana <ana@noemiweb.com.evil.com>",
            "Ana <ana@evilnoemiweb.com>",
            "Banco <ceo@banco.com> <noreply@noemiweb.com>",
            "ceo@banco.com@noemiweb.com",
    })
    void rechazaDominiosAjenosOSuplantaciones(String from) {
        assertThat(MailFromPolicy.isAllowed(from, PLATFORM)).as(from).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ceo@banco.com <noreply@noemiweb.com>",
            "Banco \"oficial\" <noreply@noemiweb.com>",
            "A<b <noreply@noemiweb.com>",
    })
    void rechazaNombresVisiblesQueSimulanOtraDireccionOCaracteresDeControl(String from) {
        assertThat(MailFromPolicy.isAllowed(from, PLATFORM)).as(from).isFalse();
    }

    @Test
    void rechazaInyeccionDeCabeceras() {
        assertThatThrownBy(() -> MailFromPolicy.validate("Ana\r\nBcc: victima@x.com <noreply@noemiweb.com>", PLATFORM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaNombresDemasiadoLargos() {
        assertThat(MailFromPolicy.isAllowed("A".repeat(MailFromPolicy.MAX_DISPLAY_NAME + 1) + " <noreply@noemiweb.com>", PLATFORM))
                .isFalse();
    }

    @Test
    void sinRemitenteDeSistemaConfigurado_noSePermiteNinguno() {
        assertThat(MailFromPolicy.isAllowed("Ana <ana@noemiweb.com>", "")).isFalse();
        assertThat(MailFromPolicy.isAllowed("Ana <ana@noemiweb.com>", null)).isFalse();
    }
}
