package com.appgestion.api.controller;

import com.appgestion.api.service.StripeWebhookService;
import com.appgestion.api.service.stripe.StripeWebhookProcessingResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        StripeWebhookProcessingResult result;
        try {
            result = stripeWebhookService.processWebhook(payload, signature);
        } catch (DataIntegrityViolationException duplicate) {
            // SEC-28: entrega concurrente del mismo evento; la otra transaccion ya lo registro/proceso.
            return ResponseEntity.ok().build();
        }
        if (result.signatureInvalid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }
        return ResponseEntity.ok().build();
    }
}
