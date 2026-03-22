package com.appgestion.api.controller;

import com.appgestion.api.dto.request.AcceptInvitacionRequest;
import com.appgestion.api.dto.request.ChangePasswordRequest;
import com.appgestion.api.dto.request.CreateInvitacionRequest;
import com.appgestion.api.dto.request.ForgotPasswordRequest;
import com.appgestion.api.dto.request.GoogleLoginRequest;
import com.appgestion.api.dto.request.LoginRequest;
import com.appgestion.api.dto.request.RegisterRequest;
import com.appgestion.api.dto.request.ResetPasswordRequest;
import com.appgestion.api.dto.request.TotpDisableRequest;
import com.appgestion.api.dto.request.TotpSetupConfirmRequest;
import com.appgestion.api.dto.request.UpdateAccountSettingsRequest;
import com.appgestion.api.dto.request.UpdatePerfilRequest;
import com.appgestion.api.dto.request.UpdatePreferenciasRequest;
import com.appgestion.api.dto.response.AuthResponse;
import com.appgestion.api.dto.response.InviteVerifyResponse;
import com.appgestion.api.dto.response.SesionDispositivoResponse;
import com.appgestion.api.dto.response.TotpSetupStartResponse;
import com.appgestion.api.dto.response.UsuarioResponse;
import com.appgestion.api.service.AuthService;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.InvitacionService;
import com.appgestion.api.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final InvitacionService invitacionService;
    private final CurrentUserService currentUserService;
    private final SessionService sessionService;

    public AuthController(AuthService authService,
                          InvitacionService invitacionService,
                          CurrentUserService currentUserService,
                          SessionService sessionService) {
        this.authService = authService;
        this.invitacionService = invitacionService;
        this.currentUserService = currentUserService;
        this.sessionService = sessionService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        AuthResponse response = authService.registrar(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request,
                                                        HttpServletRequest httpRequest) {
        AuthResponse response = authService.loginWithGoogle(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(authService.getMeResponse());
    }

    @PatchMapping("/profile")
    public ResponseEntity<UsuarioResponse> updateProfile(@Valid @RequestBody UpdatePerfilRequest request) {
        return ResponseEntity.ok(authService.actualizarMiPerfil(request));
    }

    @PatchMapping("/account-settings")
    public ResponseEntity<UsuarioResponse> updateAccountSettings(@Valid @RequestBody UpdateAccountSettingsRequest request) {
        return ResponseEntity.ok(authService.actualizarConfiguracionCuenta(request));
    }

    @PatchMapping("/preferences")
    public ResponseEntity<UsuarioResponse> updatePreferences(@Valid @RequestBody UpdatePreferenciasRequest request) {
        return ResponseEntity.ok(authService.actualizarPreferencias(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.cambiarContrasena(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/totp/setup/start")
    public ResponseEntity<TotpSetupStartResponse> totpSetupStart() {
        return ResponseEntity.ok(authService.iniciarTotpSetup());
    }

    @PostMapping("/totp/setup/confirm")
    public ResponseEntity<UsuarioResponse> totpSetupConfirm(@Valid @RequestBody TotpSetupConfirmRequest request) {
        return ResponseEntity.ok(authService.confirmarTotpSetup(request));
    }

    @PostMapping("/totp/setup/cancel")
    public ResponseEntity<Void> totpSetupCancel() {
        authService.cancelarTotpSetup();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/totp/disable")
    public ResponseEntity<Void> totpDisable(@Valid @RequestBody TotpDisableRequest request) {
        authService.desactivarTotp(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> crearInvitacion(@Valid @RequestBody CreateInvitacionRequest request) {
        invitacionService.crearInvitacion(request, currentUserService.getCurrentUsuario().getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/invite/verify")
    public InviteVerifyResponse verificarInvitacion(@RequestParam String token) {
        return invitacionService.verificar(token);
    }

    @PostMapping("/invite/accept")
    public ResponseEntity<AuthResponse> aceptarInvitacion(@Valid @RequestBody AcceptInvitacionRequest request,
                                                          HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitacionService.aceptar(request, httpRequest));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SesionDispositivoResponse>> listSessions(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(sessionService.listForCurrentUser(httpRequest));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable String sessionId, HttpServletRequest httpRequest) {
        sessionService.revokeSession(sessionId, httpRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/sessions/others")
    public ResponseEntity<Map<String, Integer>> revokeOtherSessions(HttpServletRequest httpRequest) {
        int revoked = sessionService.revokeOtherSessions(httpRequest);
        return ResponseEntity.ok(Map.of("revokedCount", revoked));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        sessionService.revokeCurrentSession(httpRequest);
        return ResponseEntity.noContent().build();
    }
}
