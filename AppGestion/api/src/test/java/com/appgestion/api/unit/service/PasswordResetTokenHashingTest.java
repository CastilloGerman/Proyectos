package com.appgestion.api.unit.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.ForgotPasswordRequest;
import com.appgestion.api.dto.request.ResetPasswordRequest;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.service.AuditAccessService;
import com.appgestion.api.service.EmailService;
import com.appgestion.api.service.PasswordResetService;
import com.appgestion.api.service.SessionService;
import com.appgestion.api.service.TotpVerificationService;
import com.appgestion.api.util.OpaqueTokens;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** SEC-06: el token de recuperación se guarda hasheado; el token en claro solo viaja en el email. */
class PasswordResetTokenHashingTest {

    private UsuarioRepository usuarioRepository;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private PasswordResetService service;
    private SessionService sessionService;
    private TotpVerificationService totpVerificationService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        emailService = mock(EmailService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        Environment env = mock(Environment.class);
        sessionService = mock(SessionService.class);
        totpVerificationService = mock(TotpVerificationService.class);
        service = new PasswordResetService(usuarioRepository, mock(EmpresaRepository.class), passwordEncoder,
                emailService, mock(AuditAccessService.class), env, sessionService, totpVerificationService);
        ReflectionTestUtils.setField(service, "frontendUrl", "https://app.example.com");

        usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setEmail("ana@example.com");
        usuario.setActivo(true);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn("hash-nuevo");
    }

    private String solicitarYExtraerTokenDelEmail() {
        service.requestPasswordReset(new ForgotPasswordRequest("ana@example.com"), null);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarPdf(any(), eq("ana@example.com"), anyString(), body.capture(), any(), any());
        Matcher m = Pattern.compile("reset-password\\?token=([A-Za-z0-9_-]+)").matcher(body.getValue());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    void enBd_soloSeGuardaElHash_yNuncaElTokenEnClaro() {
        String tokenEnClaro = solicitarYExtraerTokenDelEmail();

        String guardado = usuario.getPasswordResetToken();
        assertThat(guardado).isNotEqualTo(tokenEnClaro);
        assertThat(guardado).matches("[0-9a-f]{64}");
        assertThat(guardado).isEqualTo(OpaqueTokens.sha256Hex(tokenEnClaro));
        assertThat(tokenEnClaro).hasSize(43); // 256 bits en Base64 URL-safe
    }

    @Test
    void reset_conTokenEnClaro_buscaPorHash_yRestableceLaContrasena() {
        String tokenEnClaro = solicitarYExtraerTokenDelEmail();
        when(usuarioRepository.findByPasswordResetToken(OpaqueTokens.sha256Hex(tokenEnClaro)))
                .thenReturn(Optional.of(usuario));

        service.resetPassword(new ResetPasswordRequest(tokenEnClaro, "NuevaPassword123"), null);

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(usuario.getPasswordResetToken()).isNull();
    }

    /** SEC-08: restablecer la contraseña cierra todas las sesiones abiertas. */
    @Test
    void reset_revocaTodasLasSesionesDelUsuario() {
        String tokenEnClaro = solicitarYExtraerTokenDelEmail();
        usuario.setId(42L);
        when(usuarioRepository.findByPasswordResetToken(OpaqueTokens.sha256Hex(tokenEnClaro)))
                .thenReturn(Optional.of(usuario));

        service.resetPassword(new ResetPasswordRequest(tokenEnClaro, "NuevaPassword123"), null);

        verify(sessionService).revokeAllForUser(42L);
    }

    /** SEC-09: con 2FA activo, el reset exige código y NO consume el enlace si falta o es incorrecto. */
    @Test
    void reset_con2fa_sinCodigo_seRechazaYNoConsumeElEnlace() {
        String tokenEnClaro = solicitarYExtraerTokenDelEmail();
        usuario.setId(42L);
        usuario.setTotpEnabled(true);
        usuario.setTotpSecret("SECRETO");
        when(usuarioRepository.findByPasswordResetToken(OpaqueTokens.sha256Hex(tokenEnClaro)))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(tokenEnClaro, "NuevaPassword123"), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(PasswordResetService.TOTP_REQUERIDO);

        assertThat(usuario.getPasswordResetToken()).isNotNull();
        assertThat(usuario.getPasswordHash()).isNull();
        verify(sessionService, never()).revokeAllForUser(any());
    }

    @Test
    void reset_con2fa_codigoIncorrecto_seRechaza() {
        String tokenEnClaro = solicitarYExtraerTokenDelEmail();
        usuario.setId(42L);
        usuario.setTotpEnabled(true);
        usuario.setTotpSecret("SECRETO");
        when(usuarioRepository.findByPasswordResetToken(OpaqueTokens.sha256Hex(tokenEnClaro)))
                .thenReturn(Optional.of(usuario));
        when(totpVerificationService.verify(42L, "SECRETO", "111111")).thenReturn(false);

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(tokenEnClaro, "NuevaPassword123", "111111"), null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(usuario.getPasswordResetToken()).isNotNull();
        assertThat(usuario.getPasswordHash()).isNull();
    }

    @Test
    void reset_con2fa_codigoCorrecto_restablece() {
        String tokenEnClaro = solicitarYExtraerTokenDelEmail();
        usuario.setId(42L);
        usuario.setTotpEnabled(true);
        usuario.setTotpSecret("SECRETO");
        when(usuarioRepository.findByPasswordResetToken(OpaqueTokens.sha256Hex(tokenEnClaro)))
                .thenReturn(Optional.of(usuario));
        when(totpVerificationService.verify(42L, "SECRETO", "123456")).thenReturn(true);

        service.resetPassword(new ResetPasswordRequest(tokenEnClaro, "NuevaPassword123", "123456"), null);

        assertThat(usuario.getPasswordHash()).isEqualTo("hash-nuevo");
        assertThat(usuario.getPasswordResetToken()).isNull();
    }

    @Test
    void reset_enviandoElHashAlmacenado_noFunciona() {
        solicitarYExtraerTokenDelEmail();
        String hashAlmacenado = usuario.getPasswordResetToken();
        // Un atacante con acceso de lectura a la BD solo ve el hash: usarlo como token no debe servir.
        when(usuarioRepository.findByPasswordResetToken(OpaqueTokens.sha256Hex(hashAlmacenado)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(hashAlmacenado, "NuevaPassword123"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reset_tokenCaducado_seRechaza() {
        String tokenEnClaro = solicitarYExtraerTokenDelEmail();
        usuario.setPasswordResetExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByPasswordResetToken(OpaqueTokens.sha256Hex(tokenEnClaro)))
                .thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest(tokenEnClaro, "NuevaPassword123"), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(usuario.getPasswordResetToken()).isNull();
    }
}
