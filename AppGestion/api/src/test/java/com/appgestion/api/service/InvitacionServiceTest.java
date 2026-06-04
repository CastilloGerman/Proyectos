package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Invitacion;
import com.appgestion.api.dto.request.CreateInvitacionRequest;
import com.appgestion.api.repository.InvitacionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class InvitacionServiceTest {

    @Test
    void crearInvitacionDoesNotLogRawReferralLinkWhenEmailFails(CapturedOutput output) {
        InvitacionRepository invitacionRepository = mock(InvitacionRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        EmailService emailService = mock(EmailService.class);
        when(usuarioRepository.existsByEmail("invitee@example.com")).thenReturn(false);
        doAnswer(invocation -> {
            Invitacion invitacion = invocation.getArgument(0);
            invitacion.setId(123L);
            return invitacion;
        }).when(invitacionRepository).save(any(Invitacion.class));
        doThrow(new RuntimeException("smtp unavailable"))
                .when(emailService)
                .enviarPdf(anyLong(), any(), any(), any(), isNull(), isNull());

        InvitacionService service = new InvitacionService(
                invitacionRepository,
                usuarioRepository,
                mock(JwtService.class),
                mock(SubscriptionService.class),
                emailService,
                null,
                null,
                null,
                null);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://app.example");

        service.crearInvitacion(new CreateInvitacionRequest("Invitee@Example.com"), 99L);

        assertThat(output).contains("Invitación 123 creada pero no se pudo enviar email a invitee@example.com");
        assertThat(output).doesNotContain("https://app.example/login?ref=");
        assertThat(output).doesNotContain("ref=");
    }
}
