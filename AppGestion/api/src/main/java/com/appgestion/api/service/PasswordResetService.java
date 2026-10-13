package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.AuditAccessEventType;
import com.appgestion.api.dto.request.ForgotPasswordRequest;
import com.appgestion.api.dto.request.ResetPasswordRequest;
import com.appgestion.api.repository.EmpresaRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.util.EmailCopy;
import com.appgestion.api.util.LogSanitizer;
import com.appgestion.api.util.OpaqueTokens;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
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
    private final SessionService sessionService;
    private final TotpVerificationService totpVerificationService;

    /** Código de error (cuerpo {@code message}) que el frontend interpreta para pedir el código 2FA. */
    public static final String TOTP_REQUERIDO = "TOTP_REQUERIDO";

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.dev.log-email-links:false}")
    private boolean logEmailLinks;

    public PasswordResetService(UsuarioRepository usuarioRepository,
                                EmpresaRepository empresaRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                AuditAccessService auditAccessService,
                                Environment environment,
                                SessionService sessionService,
                                TotpVerificationService totpVerificationService) {
        this.sessionService = sessionService;
        this.totpVerificationService = totpVerificationService;
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
        // SEC-06: en BD solo se guarda el hash del token; el token en claro viaja únicamente en el enlace enviado por email.
        String token = OpaqueTokens.newToken();
        usuario.setPasswordResetToken(OpaqueTokens.sha256Hex(token));
        usuario.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(PASSWORD_RESET_EXPIRY_HOURS));
        usuarioRepository.save(usuario);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.PASSWORD_RESET_REQUESTED, true, null, true,
                httpRequest, null, "/auth/forgot-password", null);

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        // SEC-23: un enlace con token equivale a una credencial. Solo se vuelca al log si se pide expresamente en
        // desarrollo (APP_DEV_LOG_EMAIL_LINKS=true) Y el perfil es local; nunca en el resto de entornos.
        if (logEmailLinks && environment.acceptsProfiles(Profiles.of("local"))) {
            log.info("Dev: enlace de recuperación de contraseña: {}", resetLink);
        }
        String asunto = "Recuperación de contraseña - " + EmailCopy.PRODUCT_NAME;
        String nombreEmpresa = empresaRepository.findByUsuarioId(usuario.getId()).map(e -> e.getNombre()).orElse(null);
        String cuerpo = EmailCopy.prefijoDestinatarioEmpresa(usuario.getNombre(), nombreEmpresa)
                + "<p>Has solicitado restablecer tu contraseña. Haz clic en el siguiente enlace (válido " + PASSWORD_RESET_EXPIRY_HOURS + " hora(s)):</p>"
                + "<p><a href=\"" + resetLink + "\">Restablecer contraseña</a></p>"
                + "<p>Si no solicitaste este cambio, ignora este correo.</p>";

        try {
            emailService.enviarPdf(usuario.getId(), usuario.getEmail(), asunto, cuerpo, null, null);
            log.info("Email de recuperación enviado a {}", LogSanitizer.maskEmail(usuario.getEmail()));
        } catch (Exception e) {
            log.warn("No se pudo enviar email de recuperación a {}: {}", LogSanitizer.maskEmail(usuario.getEmail()),
                    e.getClass().getSimpleName());
        }
    }

    /**
     * Restablece la contraseña con el token recibido por email.
     */
    // SEC-24: noRollbackFor para que el evento de auditoría de un código 2FA incorrecto no se pierda con el rollback.
    @Transactional(noRollbackFor = {IllegalArgumentException.class, ResponseStatusException.class})
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String tokenHash = request.token() == null ? "" : OpaqueTokens.sha256Hex(request.token().trim());
        Usuario usuario = usuarioRepository.findByPasswordResetToken(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Enlace inválido o expirado"));

        if (usuario.getPasswordResetExpiresAt() == null || usuario.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            usuario.setPasswordResetToken(null);
            usuario.setPasswordResetExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("Enlace inválido o expirado");
        }

        // SEC-09: controlar el correo no basta para saltarse el 2FA. Si la cuenta lo tiene activo, se exige un código
        // válido. Se comprueba ANTES de consumir el enlace para que el usuario pueda reintentar con el mismo.
        if (Boolean.TRUE.equals(usuario.getTotpEnabled())) {
            String code = request.totpCode();
            if (code == null || code.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TOTP_REQUERIDO);
            }
            if (!totpVerificationService.verify(usuario.getId(), usuario.getTotpSecret(), code)) {
                auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "TOTP_INVALIDO",
                        false, httpRequest, null, "/auth/reset-password", Map.of("context", "PASSWORD_RESET"));
                throw new IllegalArgumentException("Código de verificación incorrecto");
            }
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        usuario.setPasswordResetToken(null);
        usuario.setPasswordResetExpiresAt(null);
        usuarioRepository.save(usuario);
        // SEC-08: quien tuviera una sesión abierta (p. ej. un atacante con un token robado) pierde el acceso.
        sessionService.revokeAllForUser(usuario.getId());
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.PASSWORD_RESET_COMPLETED, true, null, true,
                httpRequest, null, "/auth/reset-password", null);
        log.info("Contraseña restablecida para usuario id={}", usuario.getId());
        notificarCambioContrasena(usuario);
    }

    /** Avisa por email de que la contraseña se ha cambiado (best-effort: un fallo de envío no revierte el cambio). */
    public void notificarCambioContrasena(Usuario usuario) {
        try {
            String asunto = "Tu contraseña ha cambiado - " + EmailCopy.PRODUCT_NAME;
            String cuerpo = EmailCopy.prefijoDestinatarioEmpresa(usuario.getNombre(), null)
                    + "<p>Te avisamos de que la contraseña de tu cuenta se ha cambiado y que se han cerrado las demás sesiones abiertas.</p>"
                    + "<p>Si no has sido tú, restablece tu contraseña cuanto antes y contacta con soporte.</p>";
            emailService.enviarPdf(usuario.getId(), usuario.getEmail(), asunto, cuerpo, null, null);
        } catch (Exception e) {
            log.warn("No se pudo enviar el aviso de cambio de contraseña (usuario id={}): {}", usuario.getId(), e.getMessage());
        }
    }
}
