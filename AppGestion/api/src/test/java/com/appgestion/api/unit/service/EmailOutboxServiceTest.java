package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.EmailJob;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.EmailJobStatus;
import com.appgestion.api.repository.EmailJobRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.EmailJobProcessor;
import com.appgestion.api.service.EmailOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOutboxServiceTest {

    @Mock
    private EmailJobRepository emailJobRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EmailJobProcessor emailJobProcessor;

    private EmailOutboxService service;

    @BeforeEach
    void setUp() {
        service = new EmailOutboxService(
                emailJobRepository, usuarioRepository, new ObjectMapper(), emailJobProcessor);
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        lenient().when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
    }

    /** SEC-14: tope diario de correos a terceros por usuario. */
    @Test
    void rechazaEnvioACliente_cuandoSeSuperaLaCuotaDiaria() {
        ReflectionTestUtils.setField(service, "dailySendQuota", 2);
        when(emailJobRepository.countClienteJobsSince(eq(7L), any())).thenReturn(2L);

        assertThatThrownBy(() -> service.enqueueHtmlCliente(
                7L, "cliente@example.test", "ana@example.test", "Asunto", "<p>Hola</p>", null, null, "key-q"))
                .isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.getStatusCode().value()).isEqualTo(429));
        verify(emailJobRepository, never()).saveAndFlush(any());
    }

    @Test
    void permiteEnvioACliente_porDebajoDeLaCuota() {
        ReflectionTestUtils.setField(service, "dailySendQuota", 2);
        when(emailJobRepository.countClienteJobsSince(eq(7L), any())).thenReturn(1L);
        when(emailJobRepository.saveAndFlush(any(EmailJob.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        service.enqueueHtmlCliente(
                7L, "cliente@example.test", "ana@example.test", "Asunto", "<p>Hola</p>", null, null, "key-q2");

        verify(emailJobRepository).saveAndFlush(any(EmailJob.class));
    }

    @Test
    void sendsDuringTheRequestWhenResendAcceptsTheJob() {
        EmailJob[] saved = new EmailJob[1];
        when(emailJobRepository.saveAndFlush(any(EmailJob.class))).thenAnswer(invocation -> {
            EmailJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", 5L);
            saved[0] = job;
            return job;
        });
        when(emailJobRepository.findById(5L)).thenAnswer(invocation -> Optional.of(saved[0]));
        doAnswer(invocation -> {
            saved[0].setStatus(EmailJobStatus.SENT);
            return null;
        }).when(emailJobProcessor).process(5L);

        service.enqueueHtmlUsuario(7L, "ana@example.test", "Asunto", "<p>Hola</p>", "key-1");

        verify(emailJobProcessor).process(5L);
    }

    @Test
    void failsTheRequestWhenResendRejectsTheJob() {
        EmailJob[] saved = new EmailJob[1];
        when(emailJobRepository.saveAndFlush(any(EmailJob.class))).thenAnswer(invocation -> {
            EmailJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", 5L);
            saved[0] = job;
            return job;
        });
        when(emailJobRepository.findById(5L)).thenAnswer(invocation -> Optional.of(saved[0]));
        doAnswer(invocation -> {
            saved[0].setLastError("RESEND_API_KEY / app.email.resend.api-key no configurado.");
            return null;
        }).when(emailJobProcessor).process(5L);

        assertThatThrownBy(() ->
                service.enqueueHtmlUsuario(7L, "ana@example.test", "Asunto", "<p>Hola</p>", "key-1"))
                .isInstanceOf(IllegalStateException.class)
                // SEC-18: el motivo técnico (variable de entorno) no se filtra al cliente.
                .hasMessage(EmailOutboxService.MENSAJE_ENVIO_FALLIDO)
                .hasMessageNotContaining("RESEND_API_KEY");
        verify(emailJobProcessor).process(5L);
    }

    @Test
    void doesNotSendWhenTheIdempotentJobAlreadyExists() {
        when(emailJobRepository.saveAndFlush(any(EmailJob.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate"));

        service.enqueueHtmlUsuario(7L, "ana@example.test", "Asunto", "<p>Hola</p>", "key-1");

        verify(emailJobProcessor, never()).process(any());
    }
}
