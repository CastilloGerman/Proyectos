package com.appgestion.api.unit.util;

import com.appgestion.api.util.SmtpTargetValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmtpTargetValidatorTest {

    private static SmtpTargetValidator.HostResolver resolvesTo(String ip) {
        return host -> new InetAddress[] {InetAddress.getByName(ip)};
    }

    @Test
    void hostPublico_resuelvePublico_esValido() {
        assertThatCode(() -> SmtpTargetValidator.validate("smtp.gmail.com", 587, resolvesTo("142.250.185.109")))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1", "127.0.0.53", "10.0.0.5", "172.16.3.4", "192.168.1.10", "169.254.169.254",
            "100.64.0.1", "0.0.0.0", "224.0.0.1", "255.255.255.255", "::1", "fe80::1", "fc00::1", "fd12:3456::1"
    })
    void hostQueResuelveAIpNoPublica_seRechaza(String ip) {
        assertThatThrownBy(() -> SmtpTargetValidator.validate("mail.ejemplo.com", 587, resolvesTo(ip)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void siAlgunaIpEsInterna_seRechaza() {
        SmtpTargetValidator.HostResolver mixed = h -> new InetAddress[] {
                InetAddress.getByName("142.250.185.109"), InetAddress.getByName("10.1.1.1")};
        assertThatThrownBy(() -> SmtpTargetValidator.validate("mail.ejemplo.com", 587, mixed))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "localhost", "127.0.0.1", "10.0.0.1", "8.8.8.8", "[::1]", "::1", "2130706433", "0x7f.0.0.1",
            "host with spaces.com", "evil.com/../x", "evil.com:25", "a..b.com", "-bad.example.com", "",
            "metadata.google.internal.", "user@host.com"
    })
    void hostsNoPermitidos_seRechazanSinResolverDns(String host) {
        SmtpTargetValidator.HostResolver neverCalled = h -> {
            throw new AssertionError("No debería resolverse DNS para: " + h);
        };
        assertThatThrownBy(() -> SmtpTargetValidator.validate(host, 587, neverCalled))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hostNull_seRechaza() {
        assertThatThrownBy(() -> SmtpTargetValidator.validate(null, 587, resolvesTo("8.8.8.8")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dnsSinResolver_seRechaza() {
        SmtpTargetValidator.HostResolver unknown = h -> {
            throw new UnknownHostException(h);
        };
        assertThatThrownBy(() -> SmtpTargetValidator.validate("no-existe.ejemplo.com", 587, unknown))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {25, 465, 587, 2525})
    void puertosSmtpEstandar_sePermiten(int port) {
        assertThatCode(() -> SmtpTargetValidator.validatePort(port)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 22, 80, 443, 3306, 5432, 6379, 8080, 9200, 65535, -1})
    void puertosNoSmtp_seRechazan(int port) {
        assertThatThrownBy(() -> SmtpTargetValidator.validatePort(port))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void puertoNull_seInterpretaComoPorDefecto() {
        assertThatCode(() -> SmtpTargetValidator.validatePort(null)).doesNotThrowAnyException();
        assertThat(SmtpTargetValidator.ALLOWED_PORTS).contains(587);
    }
}
