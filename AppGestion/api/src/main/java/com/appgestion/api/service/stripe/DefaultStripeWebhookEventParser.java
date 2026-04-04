package com.appgestion.api.service.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.stereotype.Component;

@Component
public class DefaultStripeWebhookEventParser implements StripeWebhookEventParser {

    @Override
    public Event parse(String payload, String signature, String webhookSecret) throws SignatureVerificationException {
        return Webhook.constructEvent(payload, signature, webhookSecret);
    }
}
