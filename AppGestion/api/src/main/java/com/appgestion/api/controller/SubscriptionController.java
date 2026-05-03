package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.Usuario;
import com.appgestion.api.dto.SubscriptionBillingPeriod;
import com.appgestion.api.dto.request.CheckoutRequest;
import com.appgestion.api.dto.response.SubscriptionInvoiceDto;
import com.appgestion.api.service.CurrentUserService;
import com.appgestion.api.service.StripeService;
import com.stripe.exception.InvalidRequestException;
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
    public ResponseEntity<?> createCheckoutSession(@RequestBody(required = false) CheckoutRequest body) {
        try {
            Usuario usuario = currentUserService.getCurrentUsuario();
            SubscriptionBillingPeriod period = CheckoutRequest.effectivePeriod(body);
            String checkoutUrl = stripeService.createCheckoutSession(usuario, period);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (InvalidRequestException e) {
            String code = e.getCode();
            String msg = e.getMessage();
            log.warn("Stripe checkout invalid request: code={} param={} msg={}", code, e.getParam(), msg);
            if ("resource_missing".equals(code) && msg != null && msg.contains("price")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El ID de precio de Stripe no es válido o no existe en esta cuenta "
                                + "(revisa STRIPE_PRICE_MONTHLY / STRIPE_PRICE_YEARLY y que coincidan modo test/live)."));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Stripe rechazó la solicitud de pago. Si persiste, contacta soporte."));
        } catch (StripeException e) {
            log.warn("Stripe checkout: type={} code={} msg={}", e.getClass().getSimpleName(), e.getCode(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se pudo crear la sesión de pago. Inténtalo más tarde."));
        } catch (IllegalStateException e) {
            log.warn("Checkout configuración: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage() != null && !e.getMessage().isBlank()
                                    ? e.getMessage()
                                    : "Configuración de Stripe incompleta en el servidor."));
        } catch (Exception e) {
            log.error("Checkout inesperado", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al iniciar el pago. Inténtalo más tarde."));
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
