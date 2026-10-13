package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.EmpresaRequest;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.service.EmpresaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SEC-03: el host/puerto SMTP configurables por el usuario no pueden apuntar a la red interna ni a puertos no SMTP. */
class EmpresaServiceSmtpSsrfTest {

    private EmpresaRepository repository;
    private EmpresaService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        repository = mock(EmpresaRepository.class);
        service = new EmpresaService(repository);
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 1L);
    }

    private EmpresaRequest requestConSmtp(String host, Integer port) {
        EmpresaRequest req = mock(EmpresaRequest.class);
        when(req.mailHost()).thenReturn(host);
        when(req.mailPort()).thenReturn(port);
        return req;
    }

    private void assertRechazado(String host, Integer port) {
        assertThatThrownBy(() -> service.guardar(requestConSmtp(host, port), usuario))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repository, never()).save(any());
    }

    @Test
    void hostsInternosOInvalidos_seRechazan() {
        assertRechazado("localhost", 587);
        assertRechazado("127.0.0.1", 587);
        assertRechazado("169.254.169.254", 587);
        assertRechazado("10.0.0.5", 25);
        assertRechazado("host con espacios", 587);
    }

    @Test
    void puertosNoSmtp_seRechazan() {
        assertRechazado(null, 22);
        assertRechazado(null, 5432);
        assertRechazado(null, 6379);
    }
}
