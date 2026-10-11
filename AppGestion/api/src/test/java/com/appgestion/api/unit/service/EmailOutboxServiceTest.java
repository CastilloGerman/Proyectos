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
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
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
                .hasMessageContaining("RESEND_API_KEY");
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
