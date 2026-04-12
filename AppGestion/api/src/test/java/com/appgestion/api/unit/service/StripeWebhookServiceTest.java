package com.appgestion.api.unit.service;

import com.appgestion.api.repository.ProcessedStripeEventRepository;
import com.appgestion.api.service.DefaultStripeWebhookService;
import com.appgestion.api.service.StripeWebhookService;
import com.appgestion.api.service.SubscriptionService;
import com.appgestion.api.service.stripe.StripeSubscriptionFetcher;
import com.appgestion.api.service.stripe.StripeWebhookEventParser;
import com.appgestion.api.service.stripe.StripeWebhookProcessingResult;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock
    private StripeWebhookEventParser webhookEventParser;

    @Mock
    private StripeSubscriptionFetcher subscriptionFetcher;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ProcessedStripeEventRepository processedEventRepository;

    private StripeWebhookService stripeWebhookService;

    @BeforeEach
    void setUp() {
        stripeWebhookService = new DefaultStripeWebhookService(
                webhookEventParser,
                subscriptionFetcher,
                subscriptionService,
                processedEventRepository,
                "whsec_test_secret"
        );
    }

    @Test
    void processWebhook_invalidSignature_returnsInvalid() throws Exception {
        when(webhookEventParser.parse(any(), any(), eq("whsec_test_secret")))
                .thenThrow(mock(SignatureVerificationException.class));

        StripeWebhookProcessingResult r = stripeWebhookService.processWebhook("{}", "sig");

        assertThat(r.signatureInvalid()).isTrue();
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void processWebhook_alreadyProcessed_doesNotDispatchAgain() throws Exception {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_dup");
        when(webhookEventParser.parse(any(), any(), eq("whsec_test_secret"))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_dup")).thenReturn(true);

        StripeWebhookProcessingResult r = stripeWebhookService.processWebhook("{}", "sig");

        assertThat(r.signatureInvalid()).isFalse();
        verify(subscriptionService, never()).activateSubscription(any(), any(), any(), any(), any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void processWebhook_savesProcessedEventAfterHandling() throws Exception {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("evt_new");
        when(webhookEventParser.parse(any(), any(), eq("whsec_test_secret"))).thenReturn(event);
        when(processedEventRepository.existsByEventId("evt_new")).thenReturn(false);

        var deser = mock(com.stripe.model.EventDataObjectDeserializer.class);
        when(event.getDataObjectDeserializer()).thenReturn(deser);
        when(deser.getObject()).thenReturn(java.util.Optional.empty());

        StripeWebhookProcessingResult r = stripeWebhookService.processWebhook("{}", "sig");

        assertThat(r.signatureInvalid()).isFalse();
        verify(subscriptionService, never()).cancelSubscription(any());
        ArgumentCaptor<com.appgestion.api.domain.entity.ProcessedStripeEvent> cap =
                ArgumentCaptor.forClass(com.appgestion.api.domain.entity.ProcessedStripeEvent.class);
        verify(processedEventRepository).save(cap.capture());
        assertThat(cap.getValue().getEventId()).isEqualTo("evt_new");
    }
}
