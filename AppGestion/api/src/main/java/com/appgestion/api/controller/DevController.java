package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.service.SubscriptionService;
import com.appgestion.api.repository.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de desarrollo: solo activos con perfil "local".
 * Permite conceder premium al usuario actual para probar sin Stripe.
 */
@RestController
@RequestMapping("/dev")
@Profile("local")
public class DevController {

    private final SubscriptionService subscriptionService;
    private final UsuarioRepository usuarioRepository;

    public DevController(SubscriptionService subscriptionService, UsuarioRepository usuarioRepository) {
        this.subscriptionService = subscriptionService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Marca al usuario autenticado como ACTIVE (premium).
     * Tras llamar, haz logout y login de nuevo (o refresca /auth/me) para que el frontend reciba canWrite=true.
     */
    @PostMapping("/grant-premium")
    public ResponseEntity<Map<String, Object>> grantPremium() {
        Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
        subscriptionService.grantPremiumForDev(usuario);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Usuario marcado como premium (ACTIVE). Refresca la página o vuelve a cargar la app para que se actualice canWrite."
        ));
    }
}
