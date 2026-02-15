package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.repository.UsuarioRepository;
import com.appgestion.api.security.SecurityUtils;
import com.appgestion.api.service.StripeService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController {

    private final StripeService stripeService;
    private final UsuarioRepository usuarioRepository;

    public SubscriptionController(StripeService stripeService, UsuarioRepository usuarioRepository) {
        this.stripeService = stripeService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession() {
        try {
            Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
            String checkoutUrl = stripeService.createCheckoutSession(usuario);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/portal")
    public ResponseEntity<?> createPortalSession() {
        try {
            Usuario usuario = SecurityUtils.getCurrentUsuario(usuarioRepository);
            String stripeCustomerId = usuario.getStripeCustomerId();
            if (stripeCustomerId == null || stripeCustomerId.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No tienes una suscripción activa para gestionar"));
            }
            String portalUrl = stripeService.createBillingPortalSession(stripeCustomerId);
            return ResponseEntity.ok(Map.of("portalUrl", portalUrl));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
