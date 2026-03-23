package com.appgestion.api.service;

import com.appgestion.api.domain.enums.AuditAccessEventType;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.ChangePasswordRequest;
import com.appgestion.api.dto.request.ForgotPasswordRequest;
import com.appgestion.api.dto.request.GoogleLoginRequest;
import com.appgestion.api.dto.request.LoginRequest;
import com.appgestion.api.dto.request.RegisterRequest;
import com.appgestion.api.dto.request.ResetPasswordRequest;
import com.appgestion.api.dto.request.UpdateAccountSettingsRequest;
import com.appgestion.api.dto.request.UpdatePerfilRequest;
import com.appgestion.api.dto.request.TotpDisableRequest;
import com.appgestion.api.dto.request.TotpSetupConfirmRequest;
import com.appgestion.api.dto.request.UpdatePreferenciasRequest;
import com.appgestion.api.dto.response.AuthResponse;
import com.appgestion.api.dto.response.TotpSetupStartResponse;
import com.appgestion.api.dto.response.UsuarioResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.JwtService;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.security.TotpService;
import com.appgestion.api.validation.UserPreferencesValidator;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final LocalDate FECHA_NACIMIENTO_MIN = LocalDate.of(1900, 1, 1);

    private static final Set<String> GENEROS_PERMITIDOS = Set.of(
            "MALE", "FEMALE", "NON_BINARY", "OTHER", "UNSPECIFIED");
    private static final int TRIAL_DAYS = 14;
    private static final int PASSWORD_RESET_EXPIRY_HOURS = 1;
    private static final int TOTP_PENDING_MINUTES = 10;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;
    private final EmailService emailService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final OrganizationService organizationService;
    private final TotpService totpService;
    private final NotificacionService notificacionService;
    private final SessionService sessionService;
    private final AuditAccessService auditAccessService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SubscriptionService subscriptionService,
                       EmailService emailService,
                       GoogleTokenVerifier googleTokenVerifier,
                       OrganizationService organizationService,
                       TotpService totpService,
                       NotificacionService notificacionService,
                       SessionService sessionService,
                       AuditAccessService auditAccessService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.organizationService = organizationService;
        this.totpService = totpService;
        this.notificacionService = notificacionService;
        this.sessionService = sessionService;
        this.auditAccessService = auditAccessService;
    }

    @Transactional
    public AuthResponse registrar(RegisterRequest request, HttpServletRequest httpRequest) {
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

        usuario = organizationService.attachNewPersonalOrganization(usuario);

        notificacionService.ensureWelcomeIfEmpty(usuario.getId());

        var sesion = sessionService.createSession(usuario, httpRequest, request.clientInfo());
        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol(), sesion.getId());
        Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());

        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.REGISTER_SUCCESS, true, null, false,
                httpRequest, sesion.getId(), "/auth/register", Map.of("channel", "PASSWORD"));

        return AuthResponse.of(token, usuario.getEmail(), usuario.getRol(), expiresAt,
                usuario.getSubscriptionStatus(), usuario.getTrialEndDate(), subscriptionService.canWrite(usuario),
                sesion.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Optional<Usuario> opt = usuarioRepository.findByEmail(request.email());
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        Usuario usuario = opt.get();

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "BAD_CREDENTIALS",
                    false, httpRequest, null, "/auth/login", Map.of("channel", "PASSWORD"));
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "ACCOUNT_INACTIVE",
                    false, httpRequest, null, "/auth/login", Map.of("channel", "PASSWORD"));
            throw new IllegalArgumentException("Usuario desactivado");
        }

        subscriptionService.checkAndUpdateTrialStatus(usuario);

        requireTotpIfEnabled(usuario, request.totpCode(), httpRequest, "PASSWORD");

        notificacionService.ensureWelcomeIfEmpty(usuario.getId());

        var sesion = sessionService.createSession(usuario, httpRequest, request.clientInfo());
        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol(), sesion.getId());
        Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());

        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_SUCCESS, true, null, false, httpRequest,
                sesion.getId(), "/auth/login", Map.of("channel", "PASSWORD"));

        return AuthResponse.of(token, usuario.getEmail(), usuario.getRol(), expiresAt,
                usuario.getSubscriptionStatus(), usuario.getTrialEndDate(), subscriptionService.canWrite(usuario),
                sesion.getId());
    }

    /**
     * Inicio de sesión con Google: verifica el ID token, busca o crea usuario por email y devuelve JWT.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
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
            return organizationService.attachNewPersonalOrganization(nuevo);
        });

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "ACCOUNT_INACTIVE",
                    false, httpRequest, null, "/auth/google", Map.of("channel", "GOOGLE"));
            throw new IllegalArgumentException("Usuario desactivado");
        }

        subscriptionService.checkAndUpdateTrialStatus(usuario);

        requireTotpIfEnabled(usuario, request.totpCode(), httpRequest, "GOOGLE");

        notificacionService.ensureWelcomeIfEmpty(usuario.getId());

        var sesion = sessionService.createSession(usuario, httpRequest, request.clientInfo());
        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol(), sesion.getId());
        Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());

        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_SUCCESS, true, null, false, httpRequest,
                sesion.getId(), "/auth/google", Map.of("channel", "GOOGLE"));

        return AuthResponse.of(token, usuario.getEmail(), usuario.getRol(), expiresAt,
                usuario.getSubscriptionStatus(), usuario.getTrialEndDate(), subscriptionService.canWrite(usuario),
                sesion.getId());
    }

    public UsuarioResponse getMeResponse() {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        return usuarioToResponse(usuario);
    }

    private UsuarioResponse usuarioToResponse(Usuario usuario) {
        String stripeCustomerId = usuario.getStripeCustomerId();
        boolean billingPortal = stripeCustomerId != null && !stripeCustomerId.isBlank();
        boolean totpEnrollmentPending = usuario.getTotpPendingSecret() != null
                && usuario.getTotpPendingExpiresAt() != null
                && !usuario.getTotpPendingExpiresAt().isBefore(LocalDateTime.now());
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getRol(),
                usuario.getActivo(),
                usuario.getFechaCreacion(),
                usuario.getSubscriptionStatus() != null ? usuario.getSubscriptionStatus().name() : null,
                usuario.getTrialEndDate(),
                usuario.getSubscriptionCurrentPeriodEnd(),
                billingPortal,
                usuario.getUiLocale(),
                usuario.getTimeZone(),
                usuario.getCurrencyCode(),
                usuario.isEmailNotifyBilling(),
                usuario.isEmailNotifyDocuments(),
                usuario.isEmailNotifyMarketing(),
                subscriptionService.canWrite(usuario),
                Boolean.TRUE.equals(usuario.getTotpEnabled()),
                totpEnrollmentPending,
                usuario.getFechaNacimiento(),
                usuario.getGenero(),
                usuario.getNacionalidadIso(),
                usuario.getPaisResidenciaIso()
        );
    }

    /**
     * Si el usuario tiene 2FA activo, exige código TOTP válido. Sin código → 400 con mensaje TOTP_REQUERIDO (login / Google).
     */
    private void requireTotpIfEnabled(Usuario usuario, String totpCode, HttpServletRequest httpRequest,
                                      String channel) {
        if (!Boolean.TRUE.equals(usuario.getTotpEnabled())) {
            return;
        }
        String path = "GOOGLE".equals(channel) ? "/auth/google" : "/auth/login";
        if (totpCode == null || totpCode.isBlank()) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "TOTP_REQUERIDO",
                    false, httpRequest, null, path, Map.of("channel", channel));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOTP_REQUERIDO");
        }
        if (!totpService.verify(usuario.getTotpSecret(), totpCode)) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "TOTP_INVALIDO",
                    false, httpRequest, null, path, Map.of("channel", channel));
            throw new IllegalArgumentException("Código de verificación incorrecto");
        }
    }

    @Transactional
    public TotpSetupStartResponse iniciarTotpSetup() {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        if (Boolean.TRUE.equals(usuario.getTotpEnabled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El 2FA ya está activado");
        }
        var key = totpService.generateKey();
        usuario.setTotpPendingSecret(key.getKey());
        usuario.setTotpPendingExpiresAt(LocalDateTime.now().plusMinutes(TOTP_PENDING_MINUTES));
        usuarioRepository.save(usuario);
        String otpAuthUrl = totpService.buildOtpAuthUrl(usuario.getEmail(), key);
        return new TotpSetupStartResponse(otpAuthUrl, key.getKey(), TOTP_PENDING_MINUTES);
    }

    @Transactional
    public UsuarioResponse confirmarTotpSetup(TotpSetupConfirmRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        if (Boolean.TRUE.equals(usuario.getTotpEnabled())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El 2FA ya está activado");
        }
        String pending = usuario.getTotpPendingSecret();
        if (pending == null || usuario.getTotpPendingExpiresAt() == null) {
            throw new IllegalArgumentException("No hay un enrolamiento pendiente. Inicia de nuevo.");
        }
        if (usuario.getTotpPendingExpiresAt().isBefore(LocalDateTime.now())) {
            usuario.setTotpPendingSecret(null);
            usuario.setTotpPendingExpiresAt(null);
            usuarioRepository.save(usuario);
            throw new IllegalArgumentException("El enrolamiento ha caducado. Vuelve a generar el código QR.");
        }
        if (!totpService.verify(pending, request.code())) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "TOTP_SETUP_INVALID",
                    true, httpRequest, null, "/auth/totp/setup/confirm", Map.of("context", "TOTP_SETUP"));
            throw new IllegalArgumentException("Código incorrecto");
        }
        usuario.setTotpSecret(pending);
        usuario.setTotpEnabled(true);
        usuario.setTotpPendingSecret(null);
        usuario.setTotpPendingExpiresAt(null);
        usuarioRepository.save(usuario);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.TOTP_ENABLED, true, null, true, httpRequest,
                null, "/auth/totp/setup/confirm", null);
        return usuarioToResponse(usuario);
    }

    @Transactional
    public void cancelarTotpSetup(HttpServletRequest httpRequest) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        usuario.setTotpPendingSecret(null);
        usuario.setTotpPendingExpiresAt(null);
        usuarioRepository.save(usuario);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.TOTP_SETUP_CANCELLED, true, null, false,
                httpRequest, null, "/auth/totp/setup/cancel", null);
    }

    @Transactional
    public void desactivarTotp(TotpDisableRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        if (!Boolean.TRUE.equals(usuario.getTotpEnabled())) {
            throw new IllegalArgumentException("El 2FA no está activado");
        }
        if (!passwordEncoder.matches(request.currentPassword(), usuario.getPasswordHash())) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.PASSWORD_CHANGE_FAILURE, false,
                    "BAD_CURRENT_PASSWORD", true, httpRequest, null, "/auth/totp/disable",
                    Map.of("context", "TOTP_DISABLE"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña no es correcta");
        }
        if (!totpService.verify(usuario.getTotpSecret(), request.totpCode())) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "TOTP_INVALIDO",
                    true, httpRequest, null, "/auth/totp/disable", Map.of("context", "TOTP_DISABLE"));
            throw new IllegalArgumentException("Código de verificación incorrecto");
        }
        usuario.setTotpSecret(null);
        usuario.setTotpEnabled(false);
        usuario.setTotpPendingSecret(null);
        usuario.setTotpPendingExpiresAt(null);
        usuarioRepository.save(usuario);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.TOTP_DISABLED, true, null, true, httpRequest,
                null, "/auth/totp/disable", null);
    }

    @Transactional
    public UsuarioResponse actualizarConfiguracionCuenta(UpdateAccountSettingsRequest request) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        usuario.setEmailNotifyBilling(Boolean.TRUE.equals(request.emailNotifyBilling()));
        usuario.setEmailNotifyDocuments(Boolean.TRUE.equals(request.emailNotifyDocuments()));
        usuario.setEmailNotifyMarketing(Boolean.TRUE.equals(request.emailNotifyMarketing()));
        usuarioRepository.save(usuario);
        return usuarioToResponse(usuario);
    }

    @Transactional
    public UsuarioResponse actualizarPreferencias(UpdatePreferenciasRequest request) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        usuario.setUiLocale(UserPreferencesValidator.normalizeAndValidateLocale(request.locale()));
        usuario.setTimeZone(UserPreferencesValidator.normalizeAndValidateTimeZone(request.timeZone()));
        usuario.setCurrencyCode(UserPreferencesValidator.normalizeAndValidateCurrency(request.currencyCode()));
        usuarioRepository.save(usuario);
        return usuarioToResponse(usuario);
    }

    @Transactional
    public UsuarioResponse actualizarMiPerfil(UpdatePerfilRequest request) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        String nombre = request.nombre().trim().replaceAll("\\s+", " ");
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar 100 caracteres");
        }
        usuario.setNombre(nombre);

        String telRaw = request.telefono() != null ? request.telefono().trim() : "";
        if (telRaw.length() > 30) {
            throw new IllegalArgumentException("El teléfono no puede superar 30 caracteres");
        }
        usuario.setTelefono(telRaw.isEmpty() ? null : telRaw);

        LocalDate fn = request.fechaNacimiento();
        if (fn != null) {
            if (fn.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
            }
            if (fn.isBefore(FECHA_NACIMIENTO_MIN)) {
                throw new IllegalArgumentException("La fecha de nacimiento no es válida");
            }
        }
        usuario.setFechaNacimiento(fn);

        String genRaw = request.genero() != null ? request.genero().trim().toUpperCase() : "";
        if (!genRaw.isEmpty() && !GENEROS_PERMITIDOS.contains(genRaw)) {
            throw new IllegalArgumentException("Valor de género no válido");
        }
        usuario.setGenero(genRaw.isEmpty() ? null : genRaw);

        usuario.setNacionalidadIso(normalizarCodigoPaisIso(request.nacionalidadIso()));
        usuario.setPaisResidenciaIso(normalizarCodigoPaisIso(request.paisResidenciaIso()));

        usuarioRepository.save(usuario);
        return usuarioToResponse(usuario);
    }

    private static String normalizarCodigoPaisIso(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().toUpperCase();
        if (s.isEmpty()) {
            return null;
        }
        if (s.length() != 2 || !s.chars().allMatch(Character::isLetter)) {
            throw new IllegalArgumentException("El código de país debe ser ISO 3166-1 alpha-2");
        }
        return s;
    }

    /**
     * Solicitud de recuperación de contraseña: genera token, guarda en usuario y envía email con enlace.
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

    /**
     * Cambio de contraseña con la sesión actual (JWT). No cierra otras sesiones: el token sigue válido hasta su expiración.
     */
    @Transactional
    public void cambiarContrasena(ChangePasswordRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        String sid = sessionIdFromRequest(httpRequest);
        if (!passwordEncoder.matches(request.currentPassword(), usuario.getPasswordHash())) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.PASSWORD_CHANGE_FAILURE, false,
                    "BAD_CURRENT_PASSWORD", true, httpRequest, sid, "/auth/change-password", null);
            // 400 (no 401): evitar que clientes interpreten "sesión inválida" y cierren la sesión del usuario.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        String nueva = request.newPassword();
        if (passwordEncoder.matches(nueva, usuario.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setPasswordHash(passwordEncoder.encode(nueva));
        usuarioRepository.save(usuario);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.PASSWORD_CHANGE_SUCCESS, true, null, true,
                httpRequest, sid, "/auth/change-password", null);
        log.info("Contraseña actualizada (usuario autenticado) para {}", usuario.getEmail());
    }

    private String sessionIdFromRequest(HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            return null;
        }
        String h = httpRequest.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            return null;
        }
        return jwtService.extractSessionId(h.substring(7).trim()).orElse(null);
    }
}
