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

    public DevGrantPremiumService(AppDevProperties devProperties) {
        this.devProperties = devProperties;
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
        return !allowlist.isEmpty()
                && allowlist.contains(usuario.getEmail().trim().toLowerCase(Locale.ROOT));
    }
}
