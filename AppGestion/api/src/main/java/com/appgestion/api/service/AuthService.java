package com.appgestion.api.service;

import com.appgestion.api.constant.TrialConstants;
import com.appgestion.api.domain.enums.AuditAccessEventType;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.domain.entity.Invitacion;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.ChangePasswordRequest;
import com.appgestion.api.dto.request.ForgotPasswordRequest;
import com.appgestion.api.dto.request.GoogleLoginRequest;
import com.appgestion.api.dto.request.LoginRequest;
import com.appgestion.api.dto.request.RegisterRequest;
import com.appgestion.api.dto.request.ResendVerificationRequest;
import com.appgestion.api.dto.request.ResetPasswordRequest;
import com.appgestion.api.dto.request.VerifyEmailRequest;
import com.appgestion.api.dto.request.UpdateAccountSettingsRequest;
import com.appgestion.api.dto.request.UpdatePerfilRequest;
import com.appgestion.api.dto.request.TotpDisableRequest;
import com.appgestion.api.dto.request.TotpSetupConfirmRequest;
import com.appgestion.api.dto.request.UpdatePreferenciasRequest;
import com.appgestion.api.dto.response.AuthResponse;
import com.appgestion.api.dto.response.RegisterResponse;
import com.appgestion.api.dto.response.TotpSetupStartResponse;
import com.appgestion.api.dto.response.UsuarioResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.JwtService;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.security.TotpService;
import com.appgestion.api.validation.UserPreferencesValidator;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
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
    private static final int TOTP_PENDING_MINUTES = 10;
    /** Rol de toda cuenta autoregistrada. Los roles privilegiados nunca se asignan desde la API pública. */
    static final String ROL_USUARIO_POR_DEFECTO = "USER";
    /** Código de error (cuerpo {@code message}) que el frontend interpreta para ofrecer el reenvío del enlace. */
    public static final String EMAIL_NO_VERIFICADO = "EMAIL_NO_VERIFICADO";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final OrganizationService organizationService;
    private final TotpService totpService;
    private final TotpVerificationService totpVerificationService;
    private final NotificacionService notificacionService;
    private final SessionService sessionService;
    private final AuditAccessService auditAccessService;
    private final PresupuestoCondicionesService presupuestoCondicionesService;
    private final PasswordResetService passwordResetService;
    private final InvitacionService invitacionService;
    private final EmailVerificationService emailVerificationService;
    /** Proxy de este bean: la transacción de Google empieza después de verificar el token, no durante la red. */
    private final AuthService self;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SubscriptionService subscriptionService,
                       GoogleTokenVerifier googleTokenVerifier,
                       OrganizationService organizationService,
                       TotpService totpService,
                       TotpVerificationService totpVerificationService,
                       NotificacionService notificacionService,
                       SessionService sessionService,
                       AuditAccessService auditAccessService,
                       PresupuestoCondicionesService presupuestoCondicionesService,
                       PasswordResetService passwordResetService,
                       InvitacionService invitacionService,
                       EmailVerificationService emailVerificationService,
                       @Lazy AuthService self) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.subscriptionService = subscriptionService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.organizationService = organizationService;
        this.totpService = totpService;
        this.totpVerificationService = totpVerificationService;
        this.notificacionService = notificacionService;
        this.sessionService = sessionService;
        this.auditAccessService = auditAccessService;
        this.presupuestoCondicionesService = presupuestoCondicionesService;
        this.passwordResetService = passwordResetService;
        this.invitacionService = invitacionService;
        this.emailVerificationService = emailVerificationService;
        this.self = self;
    }

    /**
     * Alta de cuenta con email + contraseña. NO devuelve JWT: la cuenta queda pendiente de verificar el email
     * (SEC-05) y solo podrá iniciar sesión tras confirmar el enlace enviado a esa dirección.
     * <p>
     * Si el email ya existe pero su cuenta sigue SIN verificar, la cuenta se "reclama": se sustituyen nombre y
     * contraseña y se envía un nuevo enlace. Así nadie puede bloquear (ni secuestrar) el email de otra persona
     * registrándolo antes que ella (pre-hijacking).
     */
    @Transactional
    public RegisterResponse registrar(RegisterRequest request, HttpServletRequest httpRequest) {
        Invitacion referralInv = null;
        String refTok = request.referralToken();
        if (refTok != null && !refTok.isBlank()) {
            referralInv = invitacionService.assertValidInvitationForRegistration(refTok.trim());
        }

        String emailNormalized = request.email().trim().toLowerCase();
        Optional<Usuario> existente = usuarioRepository.findByEmail(emailNormalized);
        if (existente.isPresent() && existente.get().isEmailVerified()) {
            // SEC-22: respuesta idéntica a un alta nueva para no revelar qué emails están registrados. El titular real
            // recibe un aviso por email (con límite de frecuencia). Se paga el coste de bcrypt igualmente para que el
            // tiempo de respuesta no delate la rama.
            passwordEncoder.encode(request.password());
            emailVerificationService.notificarIntentoRegistro(existente.get());
            return new RegisterResponse(emailNormalized, true);
        }

        Usuario usuario = existente.orElseGet(Usuario::new);
        boolean nuevo = existente.isEmpty();
        usuario.setNombre(request.nombre());
        usuario.setEmail(emailNormalized);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(ROL_USUARIO_POR_DEFECTO); // SEC-01: el rol nunca viene del cliente
        usuario.setActivo(true);
        usuario.setEmailVerified(false);
        usuario.setReferredByUsuarioId(referralInv != null ? referralInv.getInviterUsuarioId() : null);

        if (nuevo) {
            LocalDate trialStart = LocalDate.now();
            usuario.setTrialStartDate(trialStart);
            usuario.setTrialEndDate(trialStart.plusDays(TrialConstants.DEFAULT_TRIAL_DAYS));
            usuario.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE);
            usuario = organizationService.attachNewPersonalOrganization(usuario);
            notificacionService.ensureWelcomeIfEmpty(usuario.getId());
        } else {
            usuario = usuarioRepository.save(usuario);
        }

        if (referralInv != null) {
            invitacionService.markInvitationUsed(referralInv);
        }

        emailVerificationService.sendVerificationEmail(usuario);

        Map<String, Object> auditExtras = referralInv != null
                ? Map.of("channel", "REFERRAL_TOKEN", "inviterId", referralInv.getInviterUsuarioId())
                : Map.of("channel", "PASSWORD");

        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.REGISTER_SUCCESS, true, null, false,
                httpRequest, null, "/auth/register", auditExtras);

        return new RegisterResponse(usuario.getEmail(), true);
    }

    /** Hash bcrypt ficticio (calculado una vez, con el mismo coste que los reales) para igualar tiempos de respuesta. */
    private volatile String dummyPasswordHash;

    private String dummyPasswordHash() {
        String h = dummyPasswordHash;
        if (h == null) {
            h = passwordEncoder.encode(UUID.randomUUID().toString());
            dummyPasswordHash = h;
        }
        return h;
    }

    // SEC-24: noRollbackFor para que los eventos LOGIN_FAILURE no se pierdan con el rollback de la excepción.
    @Transactional(noRollbackFor = {IllegalArgumentException.class, ResponseStatusException.class})
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Optional<Usuario> opt = usuarioRepository.findByEmail(request.email());
        if (opt.isEmpty()) {
            // SEC-22: se ejecuta un bcrypt ficticio para que "email inexistente" tarde lo mismo que "contraseña
            // incorrecta"; SEC-24: el intento queda como evento de seguridad en el log.
            passwordEncoder.matches(request.password(), dummyPasswordHash());
            auditAccessService.logAuthFailureUnknownIdentity(request.email(), "UNKNOWN_EMAIL", "/auth/login", httpRequest);
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

        // SEC-05: sin email verificado no hay sesión. Se comprueba DESPUÉS de validar la contraseña para no revelar
        // el estado de la cuenta a quien no la conoce.
        if (!usuario.isEmailVerified()) {
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.LOGIN_FAILURE, false, "EMAIL_NOT_VERIFIED",
                    false, httpRequest, null, "/auth/login", Map.of("channel", "PASSWORD"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, EMAIL_NO_VERIFICADO);
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
     * Inicio de sesión con Google. La verificación del ID token ocurre fuera de la transacción: así un fallo de red
     * con Google no retiene una conexión de base de datos hasta que el proxy corta la petición.
     */
    public AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletRequest httpRequest) {
        var tokenInfo = googleTokenVerifier.verify(request.idToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de Google inválido o expirado"));
        return self.openGoogleSession(tokenInfo, request, httpRequest);
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class, ResponseStatusException.class})
    public AuthResponse openGoogleSession(GoogleTokenVerifier.TokenInfo tokenInfo, GoogleLoginRequest request,
                                          HttpServletRequest httpRequest) {
        String email = tokenInfo.email().trim().toLowerCase();
        String nombre = tokenInfo.name() != null && !tokenInfo.name().isBlank()
                ? tokenInfo.name().trim()
                : email.substring(0, Math.min(email.indexOf('@'), email.length()));

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setNombre(nombre);
            nuevo.setEmail(email);
            nuevo.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            nuevo.setRol(ROL_USUARIO_POR_DEFECTO);
            nuevo.setActivo(true);
            nuevo.setEmailVerified(true); // Google ya ha verificado el email (tokeninfo email_verified=true)
            LocalDate trialStart = LocalDate.now();
            nuevo.setTrialStartDate(trialStart);
            nuevo.setTrialEndDate(trialStart.plusDays(TrialConstants.DEFAULT_TRIAL_DAYS));
            nuevo.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE);
            return organizationService.attachNewPersonalOrganization(nuevo);
        });

        // SEC-05 (pre-hijacking): si la cuenta existente nunca verificó su email, alguien pudo registrarla con una
        // contraseña que conoce. Google acaba de demostrar que el titular real controla ese email, así que se
        // reclama la cuenta: se invalida la contraseña previa, se cierran todas las sesiones y se marca verificada.
        if (!usuario.isEmailVerified()) {
            usuario.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            sessionService.revokeAllForUser(usuario.getId());
            emailVerificationService.marcarVerificado(usuario);
            auditAccessService.recordForUsuario(usuario, AuditAccessEventType.EMAIL_VERIFIED, true, "VERIFIED_VIA_GOOGLE",
                    true, httpRequest, null, "/auth/google", Map.of("channel", "GOOGLE"));
        }

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
                usuario.getPaisResidenciaIso(),
                presupuestoCondicionesService.desdeJson(usuario.getCondicionesPresupuestoPredeterminadas())
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
        if (!totpVerificationService.verify(usuario.getId(), usuario.getTotpSecret(), totpCode)) {
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

    @Transactional(noRollbackFor = {IllegalArgumentException.class, ResponseStatusException.class})
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
        if (!totpVerificationService.verify(usuario.getId(), pending, request.code())) {
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

    @Transactional(noRollbackFor = {IllegalArgumentException.class, ResponseStatusException.class})
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
        if (!totpVerificationService.verify(usuario.getId(), usuario.getTotpSecret(), request.totpCode())) {
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

    public void verificarEmail(VerifyEmailRequest request, HttpServletRequest httpRequest) {
        emailVerificationService.verify(request.token(), httpRequest);
    }

    public void reenviarVerificacionEmail(ResendVerificationRequest request) {
        emailVerificationService.resend(request.email());
    }

    public void requestPasswordReset(ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        passwordResetService.requestPasswordReset(request, httpRequest);
    }

    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest) {
        passwordResetService.resetPassword(request, httpRequest);
    }

    /**
     * Cambio de contraseña con la sesión actual (JWT). SEC-08: cierra todas las demás sesiones del usuario
     * (conserva solo la actual) y avisa por email del cambio.
     */
    @Transactional(noRollbackFor = {IllegalArgumentException.class, ResponseStatusException.class})
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
        sessionService.revokeAllForUserExcept(usuario.getId(), sid);
        auditAccessService.recordForUsuario(usuario, AuditAccessEventType.PASSWORD_CHANGE_SUCCESS, true, null, true,
                httpRequest, sid, "/auth/change-password", null);
        passwordResetService.notificarCambioContrasena(usuario);
        log.info("Contraseña actualizada (usuario autenticado) para usuario id={}", usuario.getId());
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
