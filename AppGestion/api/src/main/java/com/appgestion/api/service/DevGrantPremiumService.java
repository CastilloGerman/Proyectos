package com.appgestion.api.service;

import com.appgestion.api.config.AppDevProperties;
import com.appgestion.api.domain.entity.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class DevGrantPremiumService {

    private final AppDevProperties devProperties;
    private final CurrentUserService currentUserService;
    private final SubscriptionService subscriptionService;

    public DevGrantPremiumService(
            AppDevProperties devProperties,
            CurrentUserService currentUserService,
            SubscriptionService subscriptionService) {
        this.devProperties = devProperties;
        this.currentUserService = currentUserService;
        this.subscriptionService = subscriptionService;
    }

    /** Marca al usuario autenticado como ACTIVE (premium) en entornos de prueba. */
    public void grantPremiumToAuthenticatedUser() {
        Usuario usuario = currentUserService.getCurrentUsuario();
        requireCanGrant(usuario);
        subscriptionService.grantPremiumForDev(usuario);
    }

    /** Si el front debe mostrar el botón de activar premium (pruebas). */
    public boolean isAvailableForUi(Usuario usuario) {
        return devProperties.isGrantPremiumEnabled() && isEmailAllowed(usuario);
    }

    public void requireCanGrant(Usuario usuario) {
        if (!devProperties.isGrantPremiumEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!isEmailAllowed(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No autorizado para activar premium de prueba en este entorno.");
        }
    }

    private boolean isEmailAllowed(Usuario usuario) {
        if (usuario == null || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return false;
        }
        Set<String> allowlist = devProperties.grantPremiumAllowlistEmailsNormalized();
        if (allowlist.isEmpty()) {
            return true;
        }
        return allowlist.contains(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
    }
}
