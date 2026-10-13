package com.appgestion.api.service;

import com.appgestion.api.domain.entity.ProcessedStripeEvent;
import com.appgestion.api.repository.ProcessedStripeEventRepository;
import com.appgestion.api.service.stripe.StripeSubscriptionFetcher;
import com.appgestion.api.service.stripe.StripeWebhookEventParser;
import com.appgestion.api.service.stripe.StripeWebhookProcessingResult;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultStripeWebhookService implements StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(DefaultStripeWebhookService.class);

    private static final String METADATA_USUARIO_ID = "usuario_id";

    private static final String EVENT_CHECKOUT_SESSION_COMPLETED = "checkout.session.completed";
    private static final String EVENT_CUSTOMER_SUBSCRIPTION_CREATED = "customer.subscription.created";
    private static final String EVENT_CUSTOMER_SUBSCRIPTION_UPDATED = "customer.subscription.updated";
    private static final String EVENT_CUSTOMER_SUBSCRIPTION_DELETED = "customer.subscription.deleted";
    private static final String EVENT_INVOICE_PAID = "invoice.paid";
    private static final String EVENT_INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
    private static final String EVENT_INVOICE_PAYMENT_ACTION_REQUIRED = "invoice.payment_action_required";

    private static final String STRIPE_SUBSCRIPTION_PAST_DUE = "past_due";

    private final StripeWebhookEventParser webhookEventParser;
    private final StripeSubscriptionFetcher subscriptionFetcher;
    private final SubscriptionService subscriptionService;
    private final ProcessedStripeEventRepository processedEventRepository;
    private final String webhookSecret;

    public DefaultStripeWebhookService(
            StripeWebhookEventParser webhookEventParser,
            StripeSubscriptionFetcher subscriptionFetcher,
            SubscriptionService subscriptionService,
            ProcessedStripeEventRepository processedEventRepository,
            @Value("${stripe.webhook-secret}") String webhookSecret) {
        this.webhookEventParser = webhookEventParser;
        this.subscriptionFetcher = subscriptionFetcher;
        this.subscriptionService = subscriptionService;
        this.processedEventRepository = processedEventRepository;
        this.webhookSecret = webhookSecret;
    }

    @Override
    @Transactional
    public StripeWebhookProcessingResult processWebhook(String payload, String signature) {
        // SEC-28: sin secreto configurado la firma no protege nada (HMAC con clave vacía es falsificable por cualquiera),
        // así que se rechaza en TODOS los perfiles en lugar de aceptar eventos sin verificar.
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Webhook de Stripe rechazado: stripe.webhook-secret (STRIPE_WEBHOOK_SECRET) no esta configurado");
            return StripeWebhookProcessingResult.badSignature();
        }

        Event event;
        try {
            event = webhookEventParser.parse(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            return StripeWebhookProcessingResult.badSignature();
        }

        if (processedEventRepository.existsByEventId(event.getId())) {
            return StripeWebhookProcessingResult.ok();
        }

        // SEC-28: se reserva el evento ANTES de procesarlo. El indice unico sobre event_id actua de barrera: con dos
        // entregas concurrentes la segunda espera a la primera y falla con DataIntegrityViolationException (el
        // controlador la traduce a 200), en lugar de procesar el mismo evento dos veces. Si el procesamiento falla,
        // la transaccion entera (reserva incluida) se revierte y Stripe reintenta.
        ProcessedStripeEvent processed = new ProcessedStripeEvent();
        processed.setEventId(event.getId());
        processedEventRepository.save(processed);

        StripeObject dataObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (dataObject != null) {
            dispatch(event.getType(), dataObject);
        } else {
            log.warn("Stripe webhook {} ({}): objeto no deserializable (version de API distinta?); se marca como procesado sin efecto",
                    event.getId(), event.getType());
        }

        return StripeWebhookProcessingResult.ok();
    }

    private void dispatch(String type, StripeObject dataObject) {
        switch (type) {
            case EVENT_CHECKOUT_SESSION_COMPLETED -> handleCheckoutSessionCompleted((Session) dataObject);
            case EVENT_CUSTOMER_SUBSCRIPTION_CREATED, EVENT_CUSTOMER_SUBSCRIPTION_UPDATED ->
                    handleSubscriptionUpsert((Subscription) dataObject);
            case EVENT_CUSTOMER_SUBSCRIPTION_DELETED -> handleSubscriptionDeleted((Subscription) dataObject);
            case EVENT_INVOICE_PAID -> handleInvoicePaid((Invoice) dataObject);
            case EVENT_INVOICE_PAYMENT_FAILED -> handleInvoicePaymentFailed((Invoice) dataObject);
            case EVENT_INVOICE_PAYMENT_ACTION_REQUIRED -> handleInvoicePaymentActionRequired((Invoice) dataObject);
            default -> log.debug("Stripe webhook ignorado: {}", type);
        }
    }

    private void handleCheckoutSessionCompleted(Session session) {
        String usuarioIdStr = session.getMetadata() != null ? session.getMetadata().get(METADATA_USUARIO_ID) : null;
        if (usuarioIdStr == null) {
            return;
        }

        Long usuarioId;
        try {
            usuarioId = Long.valueOf(usuarioIdStr.trim());
        } catch (NumberFormatException e) {
            // SEC-28: un metadato inesperado no debe provocar un 500 (y reintentos infinitos de Stripe).
            log.warn("checkout.session.completed: metadata usuario_id no numerico; evento ignorado");
            return;
        }
        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }

        Subscription subscription;
        try {
            subscription = subscriptionFetcher.fetch(subscriptionId);
        } catch (StripeException e) {
            log.warn("checkout.session.completed: no se pudo cargar suscripción {}: {}", subscriptionId, e.getMessage());
            return;
        }

        subscriptionService.syncFromStripeSubscription(subscription, usuarioId, customerId);
    }

    private void handleSubscriptionUpsert(Subscription subscription) {
        subscriptionService.syncFromStripeSubscription(subscription, null, null);
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        subscriptionService.cancelSubscription(subscription.getId());
    }

    private void handleInvoicePaid(Invoice invoice) {
        subscriptionService.recordInvoicePaid(invoice);
    }

    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }
        subscriptionService.updateSubscription(subscriptionId, STRIPE_SUBSCRIPTION_PAST_DUE, null);
    }

    private void handleInvoicePaymentActionRequired(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null || subscriptionId.isBlank()) {
            return;
        }
        subscriptionService.markRequiresPaymentAction(subscriptionId);
    }
}
