package com.appgestion.api.service;

import com.appgestion.api.domain.entity.Invitacion;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.dto.request.AcceptInvitacionRequest;
import com.appgestion.api.dto.request.CreateInvitacionRequest;
import com.appgestion.api.dto.response.AuthResponse;
import com.appgestion.api.dto.response.InviteVerifyResponse;
import com.appgestion.api.repository.InvitacionRepository;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.JwtService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InvitacionService {

    private static final Logger log = LoggerFactory.getLogger(InvitacionService.class);
    private static final int TRIAL_DAYS = 14;
    private static final int INVITE_VALID_DAYS = 7;

    private final InvitacionRepository invitacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationService organizationService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public InvitacionService(InvitacionRepository invitacionRepository,
                             UsuarioRepository usuarioRepository,
                             JwtService jwtService,
                             SubscriptionService subscriptionService,
                             EmailService emailService,
                             PasswordEncoder passwordEncoder,
                             OrganizationService organizationService) {
        this.invitacionRepository = invitacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.subscriptionService = subscriptionService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.organizationService = organizationService;
    }

    @Transactional
    public void crearInvitacion(CreateInvitacionRequest request, Long inviterUsuarioId) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }
        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String hash = sha256Hex(rawToken);

        Invitacion inv = new Invitacion();
        inv.setEmail(email);
        inv.setTokenHash(hash);
        inv.setRol("USER");
        inv.setInviterUsuarioId(inviterUsuarioId);
        inv.setExpiresAt(LocalDateTime.now().plusDays(INVITE_VALID_DAYS));
        invitacionRepository.save(inv);

        String link = frontendUrl + "/invite/" + rawToken;
        String asunto = "Te han invitado a probar AppGestion";
        String cuerpo = "<p>Alguien te ha enviado un enlace para crear tu propia cuenta en AppGestion.</p>"
                + "<p>Tendrás un periodo de prueba; después, para seguir creando y editando necesitarás una suscripción activa.</p>"
                + "<p><a href=\"" + link + "\">Crear mi cuenta</a></p>"
                + "<p>El enlace caduca en " + INVITE_VALID_DAYS + " días.</p>";

        try {
            emailService.enviarPdf(inviterUsuarioId, email, asunto, cuerpo, null, null);
            log.info("Invitación enviada a {}", email);
        } catch (MessagingException | IllegalArgumentException | IllegalStateException e) {
            log.warn("Invitación creada pero no se pudo enviar email a {}: {}. Enlace (solo logs): {}", email, e.getMessage(), link);
        }
    }

    public InviteVerifyResponse verificar(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return InviteVerifyResponse.invalid();
        }
        String hash = sha256Hex(rawToken.trim());
        return invitacionRepository.findByTokenHash(hash)
                .filter(inv -> inv.getUsedAt() == null)
                .filter(inv -> inv.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(inv -> new InviteVerifyResponse(true, inv.getEmail()))
                .orElse(InviteVerifyResponse.invalid());
    }

    @Transactional
    public AuthResponse aceptar(AcceptInvitacionRequest request) {
        String hash = sha256Hex(request.token().trim());
        Invitacion inv = invitacionRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invitación inválida o expirada"));
        if (inv.getUsedAt() != null) {
            throw new IllegalArgumentException("Esta invitación ya fue utilizada");
        }
        if (inv.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invitación expirada");
        }
        String email = inv.getEmail();
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        Long inviterId = Objects.requireNonNull(inv.getInviterUsuarioId(), "invitación sin invitador");

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol("USER");
        usuario.setActivo(true);
        LocalDate trialStart = LocalDate.now();
        usuario.setTrialStartDate(trialStart);
        usuario.setTrialEndDate(trialStart.plusDays(TRIAL_DAYS));
        usuario.setSubscriptionStatus(SubscriptionStatus.TRIAL_ACTIVE);
        usuario.setReferredByUsuarioId(inviterId);
        usuario = organizationService.attachNewPersonalOrganization(usuario);

        inv.setUsedAt(LocalDateTime.now());
        invitacionRepository.save(inv);

        String token = jwtService.generateToken(usuario.getEmail(), usuario.getRol());
        Instant expiresAt = Instant.now().plusMillis(86400000);
        return AuthResponse.of(token, usuario.getEmail(), usuario.getRol(), expiresAt,
                usuario.getSubscriptionStatus(), usuario.getTrialEndDate(), subscriptionService.canWrite(usuario));
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
