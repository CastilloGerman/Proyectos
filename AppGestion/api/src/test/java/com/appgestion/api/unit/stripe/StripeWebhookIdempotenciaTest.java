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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StripeWebhookIdempotenciaTest {

    private static final String SECRET = "whsec_test_secret";

    private final StripeWebhookEventParser webhookEventParser;
    private final StripeSubscriptionFetcher subscriptionFetcher;
    private final SubscriptionService subscriptionService;
    private final ProcessedStripeEventRepository processedEventRepository;
    private final DefaultStripeWebhookService stripeWebhookService;

    public StripeWebhookIdempotenciaTest(
            @Mock StripeWebhookEventParser webhookEventParser,
            @Mock StripeSubscriptionFetcher subscriptionFetcher,
            @Mock SubscriptionService subscriptionService,
            @Mock ProcessedStripeEventRepository processedEventRepository) {
        this.webhookEventParser = webhookEventParser;
        this.subscriptionFetcher = subscriptionFetcher;
        this.subscriptionService = subscriptionService;
        this.processedEventRepository = processedEventRepository;
        this.stripeWebhookService = new DefaultStripeWebhookService(
                webhookEventParser,
                subscriptionFetcher,
                subscriptionService,
                processedEventRepository,
                SECRET);
    }

    @Test
    void mismoEventoDosVeces_segundaSeIgnora_sinSegundoSaveNiDispatch() throws Exception {
        Subscription subscription = org.mockito.Mockito.mock(Subscription.class);
        when(subscription.getId()).thenReturn("sub_idem");

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
        verify(subscriptionService, times(1)).syncFromStripeSubscription(eq(subscription), isNull(), isNull());
        verify(processedEventRepository, times(1)).save(any(ProcessedStripeEvent.class));

        StripeWebhookProcessingResult r2 = stripeWebhookService.processWebhook("{}", "sig");
        assertThat(r2.signatureInvalid()).isFalse();
        verify(subscriptionService, times(1)).syncFromStripeSubscription(any(), any(), any());
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
        when(deser.getObject()).thenReturn(Optional.of(subscription));

        when(webhookEventParser.parse(any(), any(), eq(SECRET))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_guardado")).thenReturn(false);

        stripeWebhookService.processWebhook("payload", "sig");

        ArgumentCaptor<ProcessedStripeEvent> cap = ArgumentCaptor.forClass(ProcessedStripeEvent.class);
        verify(processedEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventId()).isEqualTo("evt_guardado");
    }
}
