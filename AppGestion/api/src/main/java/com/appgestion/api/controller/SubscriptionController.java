package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.response.SubscriptionInvoiceDto;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.StripeService;
import com.stripe.exception.StripeException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subscription")
@Validated
public class SubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);

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
            log.debug("Stripe checkout: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo crear la sesión de pago. Inténtalo más tarde."));
        }
    }

    /**
     * Historial de facturas de la suscripción (Stripe) del usuario autenticado.
     */
    @GetMapping("/invoices")
    public ResponseEntity<?> listSubscriptionInvoices(
            @RequestParam(name = "limit", defaultValue = "24") @Min(1) @Max(100) int limit) {
        Usuario usuario = currentUserService.getCurrentUsuario();
        String customerId = usuario.getStripeCustomerId();
        if (customerId == null || customerId.isBlank()) {
            return ResponseEntity.ok(List.<SubscriptionInvoiceDto>of());
        }
        try {
            List<SubscriptionInvoiceDto> list = stripeService.listCustomerInvoices(customerId, limit);
            return ResponseEntity.ok(list);
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "No se pudo obtener el historial de facturas. Inténtalo más tarde."));
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
            log.debug("Stripe portal: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo abrir el portal de facturación. Inténtalo más tarde."));
        }
    }
}
