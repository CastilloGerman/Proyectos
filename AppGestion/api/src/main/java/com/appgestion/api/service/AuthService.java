package com.appgestion.api.service;

import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.ForgotPasswordRequest;
import com.appgestion.api.dto.request.GoogleLoginRequest;
import com.appgestion.api.dto.request.LoginRequest;
import com.appgestion.api.dto.request.RegisterRequest;
import com.appgestion.api.dto.request.ResetPasswordRequest;
import com.appgestion.api.dto.response.AuthResponse;
import com.appgestion.api.dto.response.UsuarioResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.JwtService;
import com.appgestion.api.security.SecurityUtils;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int TRIAL_DAYS = 14;
    private static final int PASSWORD_RESET_EXPIRY_HOURS = 1;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;
    private final EmailService emailService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SubscriptionService subscriptionService,
                       EmailService emailService,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.getRol());
        usuario.setActivo(true);

        LocalDate trialStart = LocalDate.now();
        usuario.setTrialStartDate(trialStart);
        usuario.setTrialEndDate(trialStart.plusDays(TRIAL_DAYS));
        usuario.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE);

        usuario = usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol());
        Instant expiresAt = Instant.now().plusMillis(86400000); // 24 horas

        return AuthResponse.of(token, usuario.getEmail(), usuario.getRol(), expiresAt,
                usuario.getSubscriptionStatus(), usuario.getTrialEndDate(), subscriptionService.canWrite(usuario));
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("Usuario desactivado");
        }

        subscriptionService.checkAndUpdateTrialStatus(usuario);

        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol());
        Instant expiresAt = Instant.now().plusMillis(86400000); // 24 horas

        return AuthResponse.of(token, usuario.getEmail(), usuario.getRol(), expiresAt,
                usuario.getSubscriptionStatus(), usuario.getTrialEndDate(), subscriptionService.canWrite(usuario));
    }

    /**
     * Inicio de sesión con Google: verifica el ID token, busca o crea usuario por email y devuelve JWT.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        var tokenInfo = googleTokenVerifier.verify(request.idToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de Google inválido o expirado"));

        String email = tokenInfo.email().trim().toLowerCase();
        String nombre = tokenInfo.name() != null && !tokenInfo.name().isBlank()
                ? tokenInfo.name().trim()
                : email.substring(0, Math.min(email.indexOf('@'), email.length()));

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setNombre(nombre);
            nuevo.setEmail(email);
            nuevo.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            nuevo.setRol("USER");
            nuevo.setActivo(true);
            LocalDate trialStart = LocalDate.now();
            nuevo.setTrialStartDate(trialStart);
            nuevo.setTrialEndDate(trialStart.plusDays(TRIAL_DAYS));
            nuevo.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE);
            return usuarioRepository.save(nuevo);
        });

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new IllegalArgumentException("Usuario desactivado");
        }

        subscriptionService.checkAndUpdateTrialStatus(usuario);
        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol());
        Instant expiresAt = Instant.now().plusMillis(86400000);

        return AuthResponse.of(token, usuario.getEmail(), usuario.getRol(), expiresAt,
                usuario.getSubscriptionStatus(), usuario.getTrialEndDate(), subscriptionService.canWrite(usuario));
    }

    public UsuarioResponse getMeResponse() {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFechaCreacion(),
                usuario.getSubscriptionStatus() != null ? usuario.getSubscriptionStatus().name() : null,
                usuario.getTrialEndDate(),
                subscriptionService.canWrite(usuario)
        );
    }

    /**
     * Solicitud de recuperación de contraseña: genera token, guarda en usuario y envía email con enlace.
     * Siempre responde éxito para no revelar si el email existe.
     */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
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

        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String asunto = "Recuperación de contraseña - AppGestion";
        String cuerpo = "<p>Hola,</p><p>Has solicitado restablecer tu contraseña. Haz clic en el siguiente enlace (válido " + PASSWORD_RESET_EXPIRY_HOURS + " hora(s)):</p>"
                + "<p><a href=\"" + resetLink + "\">Restablecer contraseña</a></p>"
                + "<p>Si no solicitaste este cambio, ignora este correo.</p>";

        try {
            emailService.enviarPdf(usuario.getId(), usuario.getEmail(), asunto, cuerpo, null, null);
            log.info("Email de recuperación enviado a {}", usuario.getEmail());
        } catch (MessagingException | IllegalArgumentException e) {
            log.warn("No se pudo enviar email de recuperación a {}: {}", usuario.getEmail(), e.getMessage());
        }
    }

    /**
     * Restablece la contraseña con el token recibido por email. Lanza si el token es inválido o ha expirado.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
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
        log.info("Contraseña restablecida para {}", usuario.getEmail());
    }
}
