package com.appgestion.api.controller;

import com.appgestion.api.dto.response.EmailOAuthStatusResponse;
import com.appgestion.api.dto.response.OAuthAuthorizeUrlResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.email.EmailOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/auth/email/oauth")
public class EmailOAuthController {

    private static final Logger log = LoggerFactory.getLogger(EmailOAuthController.class);
    private static final String OAUTH_NO_DISPONIBLE =
            "La conexión con este proveedor de correo no está disponible ahora mismo. Contacta con soporte.";

    private final EmailOAuthService emailOAuthService;
    private final CurrentUserService currentUserService;

    public EmailOAuthController(EmailOAuthService emailOAuthService, CurrentUserService currentUserService) {
        this.emailOAuthService = emailOAuthService;
        this.currentUserService = currentUserService;
    }

    /**
     * Devuelve la URL de autorización (el front redirige con window.location para conservar el JWT en la petición).
     */
    @GetMapping("/google/authorize-url")
    public OAuthAuthorizeUrlResponse googleAuthorizeUrl() {
        try {
            Long uid = currentUserService.getCurrentUsuario().getId();
            return new OAuthAuthorizeUrlResponse(emailOAuthService.buildGoogleAuthorizeUrl(uid));
        } catch (IllegalStateException e) {
            // SEC-18: el detalle de configuración (variables, redirect URI) solo va al log, no al cliente.
            log.warn("OAuth de correo no disponible: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, OAUTH_NO_DISPONIBLE);
        }
    }

    @GetMapping("/google/callback")
    public RedirectView googleCallback(@RequestParam String code, @RequestParam String state) {
        String redirect = emailOAuthService.handleGoogleCallback(code, state);
        return new RedirectView(redirect);
    }

    @GetMapping("/microsoft/authorize-url")
    public OAuthAuthorizeUrlResponse microsoftAuthorizeUrl() {
        try {
            Long uid = currentUserService.getCurrentUsuario().getId();
            return new OAuthAuthorizeUrlResponse(emailOAuthService.buildMicrosoftAuthorizeUrl(uid));
        } catch (IllegalStateException e) {
            // SEC-18: el detalle de configuración (variables, redirect URI) solo va al log, no al cliente.
            log.warn("OAuth de correo no disponible: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, OAUTH_NO_DISPONIBLE);
        }
    }

    @GetMapping("/microsoft/callback")
    public RedirectView microsoftCallback(@RequestParam String code, @RequestParam String state) {
        String redirect = emailOAuthService.handleMicrosoftCallback(code, state);
        return new RedirectView(redirect);
    }

    @DeleteMapping("/disconnect")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect() {
        emailOAuthService.disconnect(currentUserService.getCurrentUsuario().getId());
    }

    @GetMapping("/status")
    public EmailOAuthStatusResponse status() {
        return emailOAuthService.status(currentUserService.getCurrentUsuario().getId());
    }
}
