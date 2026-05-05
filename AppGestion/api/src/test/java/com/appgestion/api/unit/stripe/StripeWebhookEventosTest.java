package com.appgestion.api.unit.stripe;

import com.appgestion.api.domain.entity.ProcessedStripeEvent;
import com.appgestion.api.repository.ProcessedStripeEventRepository;
import com.appgestion.api.service.DefaultStripeWebhookService;
import com.appgestion.api.service.SubscriptionService;
import com.appgestion.api.service.stripe.StripeSubscriptionFetcher;
import com.appgestion.api.service.stripe.StripeWebhookEventParser;
import com.appgestion.api.service.stripe.StripeWebhookProcessingResult;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookEventosTest {

    private static final String SECRET = "whsec_test_secret";

    @Mock
    private StripeWebhookEventParser webhookEventParser;
    @Mock
    private StripeSubscriptionFetcher subscriptionFetcher;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private ProcessedStripeEventRepository processedEventRepository;

    private DefaultStripeWebhookService stripeWebhookService;

    @BeforeEach
    void setUp() {
        stripeWebhookService = new DefaultStripeWebhookService(
                webhookEventParser,
                subscriptionFetcher,
                subscriptionService,
                processedEventRepository,
                SECRET
        );
    }

    @Test
    void customerSubscriptionUpdated_actualizaPlan() throws Exception {
        Subscription subscription = org.mockito.Mockito.mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub_upd");
        when(subscription.getStatus()).thenReturn("past_due");
        long end = 1_900_000_000L;
        when(subscription.getCurrentPeriodEnd()).thenReturn(end);

        Event event = eventWith("evt_upd", "customer.subscription.updated", subscription);

        when(webhookEventParser.parse(any(), any(), eq(SECRET))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_upd")).thenReturn(false);

        StripeWebhookProcessingResult r = stripeWebhookService.processWebhook("{}", "sig");
        assertThat(r.signatureInvalid()).isFalse();

        verify(subscriptionService).updateSubscription(
                "sub_upd", "past_due", Instant.ofEpochSecond(end));
        verify(subscriptionFetcher, never()).fetch(any());
    }

    @Test
    void customerSubscriptionDeleted_marcaCancelada() throws Exception {
        Subscription subscription = org.mockito.Mockito.mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub_del");

        Event event = eventWith("evt_del", "customer.subscription.deleted", subscription);

        when(webhookEventParser.parse(any(), any(), eq(SECRET))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_del")).thenReturn(false);

        stripeWebhookService.processWebhook("{}", "sig");

        verify(subscriptionService).cancelSubscription("sub_del");
        verify(subscriptionService, never()).updateSubscription(any(), any(), any());
    }

    /**
     * La API usa el tipo {@code invoice.paid} (no {@code invoice.payment_succeeded}).
     */
    @Test
    void invoicePaid_registraPagoActivo() throws Exception {
        Invoice invoice = org.mockito.Mockito.mock(Invoice.class);
        when(invoice.getSubscription()).thenReturn("sub_inv");

        Event event = eventWith("evt_inv", "invoice.paid", invoice);

        when(webhookEventParser.parse(any(), any(), eq(SECRET))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_inv")).thenReturn(false);

        stripeWebhookService.processWebhook("{}", "sig");

        verify(subscriptionService).updateSubscription(eq("sub_inv"), eq("active"), isNull());
    }

    @Test
    void checkoutCompleted_siStripeNoDevuelveSuscripcion_noMarcaEventoProcesado() throws Exception {
        Session session = org.mockito.Mockito.mock(Session.class);
        when(session.getId()).thenReturn("cs_retry");
        when(session.getMetadata()).thenReturn(Map.of("usuario_id", "42"));
        when(session.getCustomer()).thenReturn("cus_retry");
        when(session.getSubscription()).thenReturn("sub_retry");

        Event event = eventWith("evt_retry", "checkout.session.completed", session);

        when(webhookEventParser.parse(any(), any(), eq(SECRET))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_retry")).thenReturn(false);
        when(subscriptionFetcher.fetch("sub_retry")).thenThrow(mock(com.stripe.exception.StripeException.class));

        assertThatThrownBy(() -> stripeWebhookService.processWebhook("{}", "sig"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("suscripción Stripe");

        verify(subscriptionService, never()).activateSubscription(any(), any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any(ProcessedStripeEvent.class));
    }

    @Test
    void eventoDesconocido_seIgnoraSinError_yRegistraProcessedEvent() throws Exception {
        StripeObject data = org.mockito.Mockito.mock(StripeObject.class);
        Event event = eventWith("evt_ping", "ping", data);

        when(webhookEventParser.parse(any(), any(), eq(SECRET))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_ping")).thenReturn(false);

        StripeWebhookProcessingResult r = stripeWebhookService.processWebhook("{}", "sig");
        assertThat(r.signatureInvalid()).isFalse();

        verify(subscriptionService, never()).activateSubscription(any(), any(), any(), any(), any());
        verify(subscriptionService, never()).updateSubscription(any(), any(), any());
        verify(subscriptionService, never()).cancelSubscription(any());

        ArgumentCaptor<ProcessedStripeEvent> cap = ArgumentCaptor.forClass(ProcessedStripeEvent.class);
        verify(processedEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventId()).isEqualTo("evt_ping");
    }

    private static Event eventWith(String id, String type, StripeObject dataObject) {
        Event event = org.mockito.Mockito.mock(Event.class);
        when(event.getId()).thenReturn(id);
        when(event.getType()).thenReturn(type);
        EventDataObjectDeserializer deser = org.mockito.Mockito.mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deser);
        when(deser.getObject()).thenReturn(Optional.of(dataObject));
        return event;
    }
}
