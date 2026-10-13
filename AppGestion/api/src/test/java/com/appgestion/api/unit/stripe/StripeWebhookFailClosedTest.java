package com.appgestion.api.unit.stripe;

import com.appgestion.api.repository.ProcessedStripeEventRepository;
import com.appgestion.api.service.DefaultStripeWebhookService;
import com.appgestion.api.service.SubscriptionService;
import com.appgestion.api.service.stripe.StripeSubscriptionFetcher;
import com.appgestion.api.service.stripe.StripeWebhookEventParser;
import com.appgestion.api.service.stripe.StripeWebhookProcessingResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** SEC-28: sin secreto de webhook configurado, ningún evento se acepta (fail-closed en todos los perfiles). */
class StripeWebhookFailClosedTest {

    @Test
    void secretoVacio_rechazaElEventoSinParsearniProcesar() throws Exception {
        StripeWebhookEventParser parser = mock(StripeWebhookEventParser.class);
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        ProcessedStripeEventRepository processed = mock(ProcessedStripeEventRepository.class);
        DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                parser, mock(StripeSubscriptionFetcher.class), subscriptions, processed, "");

        StripeWebhookProcessingResult r = service.processWebhook("{\"type\":\"invoice.paid\"}", "cualquier-firma");

        assertThat(r.signatureInvalid()).isTrue();
        verifyNoInteractions(parser, subscriptions);
        verify(processed, never()).save(any());
    }

    @Test
    void secretoNulo_tambienSeRechaza() {
        DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                mock(StripeWebhookEventParser.class), mock(StripeSubscriptionFetcher.class),
                mock(SubscriptionService.class), mock(ProcessedStripeEventRepository.class), null);

        assertThat(service.processWebhook("{}", "sig").signatureInvalid()).isTrue();
    }
}
