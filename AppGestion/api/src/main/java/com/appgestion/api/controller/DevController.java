package com.appgestion.api.controller;

import com.appgestion.api.service.DevGrantPremiumService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de prueba en entornos desplegados.
 * Activos solo si {@code app.dev.grant-premium-enabled=true} (variable {@code APP_DEV_GRANT_PREMIUM_ENABLED}).
 */
@RestController
@RequestMapping("/dev")
@ConditionalOnProperty(prefix = "app.dev", name = "grant-premium-enabled", havingValue = "true")
public class DevController {

    private final DevGrantPremiumService devGrantPremiumService;

    public DevController(DevGrantPremiumService devGrantPremiumService) {
        this.devGrantPremiumService = devGrantPremiumService;
    }

    /**
     * Marca al usuario autenticado como ACTIVE (premium).
     * Tras llamar, refresca /auth/me para que el frontend reciba canWrite=true.
     */
    @PostMapping("/grant-premium")
    public ResponseEntity<Map<String, Object>> grantPremium() {
        devGrantPremiumService.grantPremiumToAuthenticatedUser();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Usuario marcado como premium (ACTIVE). Refresca la página o vuelve a cargar la app para que se actualice canWrite."
        ));
    }
}
