package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.StripeService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController {

    private final StripeService stripeService;
    private final CurrentUserService currentUserService;

    public SubscriptionController(StripeService stripeService, CurrentUserService currentUserService) {
        this.stripeService = stripeService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession() {
        try {
            Usuario usuario = currentUserService.getCurrentUsuario();
            String checkoutUrl = stripeService.createCheckoutSession(usuario);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (StripeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/portal")
    public ResponseEntity<?> createPortalSession() {
        try {
            Usuario usuario = currentUserService.getCurrentUsuario();
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
