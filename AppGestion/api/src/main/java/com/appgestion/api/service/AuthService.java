package com.appgestion.api.service;

import com.appgestion.api.domain.enums.SubscriptionStatus;
import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.request.LoginRequest;
import com.appgestion.api.dto.request.RegisterRequest;
import com.appgestion.api.dto.response.AuthResponse;
import com.appgestion.api.dto.response.UsuarioResponse;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class AuthService {

    private static final int TRIAL_DAYS = 14;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SubscriptionService subscriptionService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.subscriptionService = subscriptionService;
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
}
