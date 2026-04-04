package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.AuditAccessEventType;
import com.appgestion.api.dto.request.ForgotPasswordRequest;
import com.appgestion.api.dto.request.ResetPasswordRequest;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.util.EmailCopy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Solicitud y confirmación de recuperación de contraseña por email.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private static final int PASSWORD_RESET_EXPIRY_HOURS = 1;

    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditAccessService auditAccessService;
    private final Environment environment;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public PasswordResetService(UsuarioRepository usuarioRepository,
                                EmpresaRepository empresaRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                AuditAccessService auditAccessService,
                                Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.auditAccessService = auditAccessService;
        this.environment = environment;
    }

    /**
     * Genera token, guarda en usuario y envía email con enlace.
     * Siempre responde éxito para no revelar si el email existe.
     */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        Optional<Usuario> opt = usuarioRepository.findByEmail(request.email().trim());
        if (opt.isEmpty()) {
            return;
        }
        Usuario usuario = opt.get();
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            return;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        usuario.setPasswordResetToken(token);
        usuario.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(PASSWORD_RESET_EXPIRY_HOURS));
        usuarioRepository.save(usuario);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.PASSWORD_RESET_REQUESTED, true, null, true,
                httpRequest, null, "/auth/forgot-password", null);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        if (environment.acceptsProfiles(Profiles.of("local"))) {
            log.info("Perfil local: enlace de recuperación de contraseña (útil si Resend sandbox no entrega el correo): {}", resetLink);
        }
        String asunto = "Recuperación de contraseña - " + EmailCopy.PRODUCT_NAME;
        String nombreEmpresa = empresaRepository.findByUsuarioId(usuario.getId()).map(e -> e.getNombre()).orElse(null);
        String cuerpo = EmailCopy.prefijoDestinatarioEmpresa(usuario.getNombre(), nombreEmpresa)
                + "<p>Has solicitado restablecer tu contraseña. Haz clic en el siguiente enlace (válido " + PASSWORD_RESET_EXPIRY_HOURS + " hora(s)):</p>"
                + "<p><a href=\"" + resetLink + "\">Restablecer contraseña</a></p>"
                + "<p>Si no solicitaste este cambio, ignora este correo.</p>";

        try {
            emailService.enviarPdf(usuario.getId(), usuario.getEmail(), asunto, cuerpo, null, null);
            log.info("Email de recuperación enviado a {}", usuario.getEmail());
        } catch (Exception e) {
            log.warn("No se pudo enviar email de recuperación a {}: {}", usuario.getEmail(), e.getMessage());
        }
    }

    /**
     * Restablece la contraseña con el token recibido por email.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = usuarioRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Enlace inválido o expirado"));

        if (usuario.getPasswordResetExpiresAt() == null || usuario.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            usuario.setPasswordResetToken(null);
            usuario.setPasswordResetExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("Enlace inválido o expirado");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        usuario.setPasswordResetToken(null);
        usuario.setPasswordResetExpiresAt(null);
        usuarioRepository.save(usuario);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.PASSWORD_RESET_COMPLETED, true, null, true,
                httpRequest, null, "/auth/reset-password", null);
        log.info("Contraseña restablecida para {}", usuario.getEmail());
    }
}
