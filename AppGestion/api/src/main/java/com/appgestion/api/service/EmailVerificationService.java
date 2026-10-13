package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.AuditAccessEventType;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.util.EmailCopy;
import com.appgestion.api.util.OpaqueTokens;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Verificación de la propiedad del email (SEC-05). El token en claro solo viaja en el enlace enviado por email;
 * en BD se guarda su hash SHA-256. Hasta verificar, la cuenta no puede iniciar sesión con contraseña.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    static final int VERIFICATION_VALID_HOURS = 24;
    static final int RESEND_COOLDOWN_SECONDS = 60;

    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final AuditAccessService auditAccessService;
    private final Environment environment;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.dev.log-email-links:false}")
    private boolean logEmailLinks;

    public EmailVerificationService(UsuarioRepository usuarioRepository,
                                    EmailService emailService,
                                    AuditAccessService auditAccessService,
                                    Environment environment) {
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.auditAccessService = auditAccessService;
        this.environment = environment;
    }

    /** Genera un token nuevo (invalida el anterior), lo guarda hasheado y envía el email con el enlace. */
    @Transactional
    public void sendVerificationEmail(Usuario usuario) {
        String token = OpaqueTokens.newToken();
        LocalDateTime now = LocalDateTime.now();
        usuario.setEmailVerificationTokenHash(OpaqueTokens.sha256Hex(token));
        usuario.setEmailVerificationExpiresAt(now.plusHours(VERIFICATION_VALID_HOURS));
        usuario.setEmailVerificationSentAt(now);
        usuarioRepository.save(usuario);

        String link = frontendUrl.replaceAll("/+$", "") + "/verify-email?token=" + token;
        // SEC-23: el enlace es una credencial de un solo uso; solo se registra con opt-in explícito en perfil local.
        if (logEmailLinks && environment.acceptsProfiles(Profiles.of("local"))) {
            log.info("Dev: enlace de verificación de email: {}", link);
        }
        String asunto = "Confirma tu email - " + EmailCopy.PRODUCT_NAME;
        String cuerpo = EmailCopy.prefijoDestinatarioEmpresa(usuario.getNombre(), null)
                + "<p>Gracias por registrarte. Confirma que este email es tuyo haciendo clic en el siguiente enlace "
                + "(válido " + VERIFICATION_VALID_HOURS + " horas):</p>"
                + "<p><a href=\"" + link + "\">Confirmar mi email</a></p>"
                + "<p>Si no has creado una cuenta, ignora este correo.</p>";
        try {
            emailService.enviarPdf(usuario.getId(), usuario.getEmail(), asunto, cuerpo, null, null);
        } catch (Exception e) {
            log.warn("No se pudo encolar el email de verificación (usuarioId={}): {}", usuario.getId(), e.getMessage());
        }
    }

    /** Confirma el email con el token recibido. */
    @Transactional
    public void verify(String rawToken, HttpServletRequest httpRequest) {
        String hash = rawToken == null ? "" : OpaqueTokens.sha256Hex(rawToken.trim());
        Usuario usuario = usuarioRepository.findByEmailVerificationTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Enlace inválido o expirado"));
        if (usuario.getEmailVerificationExpiresAt() == null
                || usuario.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            usuario.setEmailVerificationTokenHash(null);
            usuario.setEmailVerificationExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("Enlace inválido o expirado");
        }
        marcarVerificado(usuario);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.EMAIL_VERIFIED, true, null, true,
                httpRequest, null, "/auth/verify-email", null);
    }

    /**
     * Reenvío del email de verificación. Nunca revela si la cuenta existe: siempre termina sin error.
     * Respeta un cooldown para no usar el servicio como relé de spam.
     */
    @Transactional
    public void resend(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Optional<Usuario> opt = usuarioRepository.findByEmail(email.trim().toLowerCase());
        if (opt.isEmpty()) {
            return;
        }
        Usuario usuario = opt.get();
        if (usuario.isEmailVerified() || !Boolean.TRUE.equals(usuario.getActivo())) {
            return;
        }
        LocalDateTime last = usuario.getEmailVerificationSentAt();
        if (last != null && last.isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
            return;
        }
        sendVerificationEmail(usuario);
    }

    /** Un aviso por cuenta y hora como máximo: evita usar el registro para bombardear a un titular con correos. */
    private final Cache<Long, Boolean> avisoRegistroEnfriamiento = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    /**
     * SEC-22: alguien intentó registrar un email que ya tiene cuenta verificada. El alta responde igual que una nueva
     * (para no revelar qué emails existen), así que se avisa al titular real por correo, con límite de frecuencia.
     */
    public void notificarIntentoRegistro(Usuario usuario) {
        if (usuario == null || avisoRegistroEnfriamiento.asMap().putIfAbsent(usuario.getId(), Boolean.TRUE) != null) {
            return;
        }
        try {
            String link = frontendUrl.replaceAll("/+$", "") + "/forgot-password";
            String asunto = "Ya tienes una cuenta - " + EmailCopy.PRODUCT_NAME;
            String cuerpo = EmailCopy.prefijoDestinatarioEmpresa(usuario.getNombre(), null)
                    + "<p>Alguien ha intentado crear una cuenta con este email, pero ya tienes una. "
                    + "Si has sido tú, inicia sesión con tu contraseña habitual o "
                    + "<a href=\"" + link + "\">restablécela aquí</a>.</p>"
                    + "<p>Si no has sido tú, puedes ignorar este correo: tu cuenta no ha sufrido ningún cambio.</p>";
            emailService.enviarPdf(usuario.getId(), usuario.getEmail(), asunto, cuerpo, null, null);
        } catch (Exception e) {
            log.warn("No se pudo enviar el aviso de intento de registro (usuarioId={}): {}", usuario.getId(),
                    e.getClass().getSimpleName());
        }
    }

    /** Marca el email como verificado y descarta cualquier token pendiente. */
    public void marcarVerificado(Usuario usuario) {
        usuario.setEmailVerified(true);
        usuario.setEmailVerificationTokenHash(null);
        usuario.setEmailVerificationExpiresAt(null);
        usuarioRepository.save(usuario);
    }
}
