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
import com.stripe.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeWebhookIdempotenciaTest {

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
    void mismoEventoDosVeces_segundaSeIgnora_sinSegundoSaveNiDispatch() throws Exception {
        Subscription subscription = org.mockito.Mockito.mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub_idem");
        when(subscription.getStatus()).thenReturn("active");
        long periodEnd = 1_700_000_000L;
        when(subscription.getCurrentPeriodEnd()).thenReturn(periodEnd);

        Event event = org.mockito.Mockito.mock(Event.class);
        when(event.getId()).thenReturn("evt_idem");
        when(event.getType()).thenReturn("customer.subscription.updated");
        EventDataObjectDeserializer deser = org.mockito.Mockito.mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deser);
        when(deser.getObject()).thenReturn(Optional.of(subscription));

        when(webhookEventParser.parse(any(), any(), eq(SECRET))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_idem")).thenReturn(false, true);

        StripeWebhookProcessingResult r1 = stripeWebhookService.processWebhook("{}", "sig");
        assertThat(r1.signatureInvalid()).isFalse();
        verify(subscriptionService, times(1)).updateSubscription(
                eq("sub_idem"), eq("active"), eq(Instant.ofEpochSecond(periodEnd)));
        verify(processedEventRepository, times(1)).save(any(ProcessedStripeEvent.class));

        StripeWebhookProcessingResult r2 = stripeWebhookService.processWebhook("{}", "sig");
        assertThat(r2.signatureInvalid()).isFalse();
        verify(subscriptionService, times(1)).updateSubscription(any(), any(), any());
        verify(processedEventRepository, times(1)).save(any(ProcessedStripeEvent.class));
        verify(subscriptionFetcher, never()).fetch(any());
    }

    @Test
    void processedStripeEvent_guardaEventIdCorrecto() throws Exception {
        Event event = org.mockito.Mockito.mock(Event.class);
        when(event.getId()).thenReturn("evt_guardado");
        when(event.getType()).thenReturn("customer.subscription.updated");
        EventDataObjectDeserializer deser = org.mockito.Mockito.mock(EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deser);
        Subscription subscription = org.mockito.Mockito.mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub_x");
        when(subscription.getStatus()).thenReturn("active");
        when(subscription.getCurrentPeriodEnd()).thenReturn(1_800_000_000L);
        when(deser.getObject()).thenReturn(Optional.of(subscription));

        when(webhookEventParser.parse(any(), any(), eq(SECRET))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_guardado")).thenReturn(false);

        stripeWebhookService.processWebhook("payload", "sig");

        ArgumentCaptor<ProcessedStripeEvent> cap = ArgumentCaptor.forClass(ProcessedStripeEvent.class);
        verify(processedEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventId()).isEqualTo("evt_guardado");
    }
}
