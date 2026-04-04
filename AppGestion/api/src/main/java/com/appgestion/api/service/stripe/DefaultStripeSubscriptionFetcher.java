package com.appgestion.api.service.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import org.springframework.stereotype.Component;

@Component
public class DefaultStripeSubscriptionFetcher implements StripeSubscriptionFetcher {

    @Override
    public Subscription fetch(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }
}
