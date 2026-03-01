package com.appgestion.api.controller;

import com.appgestion.api.domain.entity.ProcessedStripeEvent;
import com.appgestion.api.repository.ProcessedStripeEventRepository;
import com.appgestion.api.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/webhook")
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final SubscriptionService subscriptionService;
    private final ProcessedStripeEventRepository processedEventRepository;

    public StripeWebhookController(SubscriptionService subscriptionService,
                                   ProcessedStripeEventRepository processedEventRepository) {
        this.subscriptionService = subscriptionService;
        this.processedEventRepository = processedEventRepository;
    }

    @PostMapping("/stripe")
    @Transactional
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        if (processedEventRepository.existsByEventId(event.getId())) {
            return ResponseEntity.ok().build();
        }

        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (dataObject == null) {
            return ResponseEntity.ok().build();
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted((Session) dataObject);
            case "customer.subscription.updated" -> handleSubscriptionUpdated((Subscription) dataObject);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted((Subscription) dataObject);
            case "invoice.paid" -> handleInvoicePaid((Invoice) dataObject);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed((Invoice) dataObject);
            default -> { /* Ignore other events */ }
        }

        ProcessedStripeEvent processed = new ProcessedStripeEvent();
        processed.setEventId(event.getId());
        processedEventRepository.save(processed);

        return ResponseEntity.ok().build();
    }

    private void handleCheckoutSessionCompleted(Session session) {
        String usuarioIdStr = session.getMetadata() != null ? session.getMetadata().get("usuario_id") : null;
        if (usuarioIdStr == null) return;

        Long usuarioId = Long.valueOf(usuarioIdStr);
        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        if (subscriptionId == null || subscriptionId.isBlank()) return;

        Subscription subscription;
        try {
            subscription = Subscription.retrieve(subscriptionId);
        } catch (StripeException e) {
            return;
        }

        String status = subscription.getStatus();
        Instant periodEnd = subscription.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(subscription.getCurrentPeriodEnd())
                : null;

        subscriptionService.activateSubscription(usuarioId, customerId, subscriptionId, status, periodEnd);
    }

    private void handleSubscriptionUpdated(Subscription subscription) {
        String subscriptionId = subscription.getId();
        String status = subscription.getStatus();
        Instant periodEnd = subscription.getCurrentPeriodEnd() != null
                ? Instant.ofEpochSecond(subscription.getCurrentPeriodEnd())
                : null;

        subscriptionService.updateSubscription(subscriptionId, status, periodEnd);
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        subscriptionService.cancelSubscription(subscription.getId());
    }

    private void handleInvoicePaid(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null || subscriptionId.isBlank()) return;
        subscriptionService.updateSubscription(subscriptionId, "active", null);
    }

    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null || subscriptionId.isBlank()) return;
        subscriptionService.updateSubscription(subscriptionId, "past_due", null);
    }
}
