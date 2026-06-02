package com.appgestion.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Herramientas de prueba en entornos desplegados (p. ej. activar premium sin Stripe).
 * Desactivado por defecto; activar solo con {@code APP_DEV_GRANT_PREMIUM_ENABLED=true}.
 */
@ConfigurationProperties(prefix = "app.dev")
public class AppDevProperties {

    private boolean grantPremiumEnabled = false;

    /**
     * Emails permitidos (separados por coma, sin distinguir mayúsculas).
     * Vacío = cualquier usuario autenticado puede usar la función si está habilitada.
     */
    private String grantPremiumEmailAllowlist = "";

    public boolean isGrantPremiumEnabled() {
        return grantPremiumEnabled;
    }

    public void setGrantPremiumEnabled(boolean grantPremiumEnabled) {
        this.grantPremiumEnabled = grantPremiumEnabled;
    }

    public String getGrantPremiumEmailAllowlist() {
        return grantPremiumEmailAllowlist;
    }

    public void setGrantPremiumEmailAllowlist(String grantPremiumEmailAllowlist) {
        this.grantPremiumEmailAllowlist = grantPremiumEmailAllowlist;
    }

    public Set<String> grantPremiumAllowlistEmailsNormalized() {
        if (grantPremiumEmailAllowlist == null || grantPremiumEmailAllowlist.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(grantPremiumEmailAllowlist.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
