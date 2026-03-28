package com.appgestion.api.controller;

import com.appgestion.api.dto.response.EmailOAuthStatusResponse;
import com.appgestion.api.dto.response.OAuthAuthorizeUrlResponse;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.email.EmailOAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/auth/email/oauth")
public class EmailOAuthController {

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
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
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
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
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
