package com.appgestion.api.unit.service;

import com.appgestion.api.security.TotpService;
import com.appgestion.api.service.TotpVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** SEC-12: bloqueo por intentos fallidos y rechazo de códigos reutilizados. */
class TotpVerificationServiceTest {

    private static final Long USER = 7L;
    private static final String SECRET = "SECRETO";

    private TotpService totpService;
    private TotpVerificationService service;

    @BeforeEach
    void setUp() {
        totpService = mock(TotpService.class);
        service = new TotpVerificationService(totpService);
        when(totpService.verify(SECRET, "123456")).thenReturn(true);
        when(totpService.verify(SECRET, "000000")).thenReturn(false);
    }

    @Test
    void codigoValido_seAcepta() {
        assertThat(service.verify(USER, SECRET, "123456")).isTrue();
    }

    @Test
    void mismoCodigoDosVeces_laSegundaSeRechaza_replay() {
        assertThat(service.verify(USER, SECRET, "123456")).isTrue();
        assertThat(service.verify(USER, SECRET, " 123456 ")).isFalse();
    }

    @Test
    void tras5Fallos_seBloqueaAunqueElCodigoSeaCorrecto() {
        for (int i = 0; i < TotpVerificationService.MAX_FAILED_ATTEMPTS; i++) {
            assertThat(service.verify(USER, SECRET, "000000")).isFalse();
        }
        assertThatThrownBy(() -> service.verify(USER, SECRET, "123456"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void elBloqueoEsPorUsuario() {
        for (int i = 0; i < TotpVerificationService.MAX_FAILED_ATTEMPTS; i++) {
            service.verify(USER, SECRET, "000000");
        }
        assertThat(service.verify(8L, SECRET, "123456")).isTrue();
    }

    @Test
    void unAciertoReiniciaElContadorDeFallos() {
        for (int i = 0; i < TotpVerificationService.MAX_FAILED_ATTEMPTS - 1; i++) {
            service.verify(USER, SECRET, "000000");
        }
        assertThat(service.verify(USER, SECRET, "123456")).isTrue();
        // De nuevo hay margen completo para equivocarse sin bloqueo.
        for (int i = 0; i < TotpVerificationService.MAX_FAILED_ATTEMPTS - 1; i++) {
            assertThat(service.verify(USER, SECRET, "000000")).isFalse();
        }
    }

    @Test
    void usuarioNulo_seRechaza() {
        assertThat(service.verify(null, SECRET, "123456")).isFalse();
    }
}
