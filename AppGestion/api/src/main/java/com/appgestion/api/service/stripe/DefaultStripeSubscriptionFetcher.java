package com.appgestion.api.service.stripe;

import com.appgestion.api.config.StripeConfig;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import org.springframework.stereotype.Component;

@Component
public class DefaultStripeSubscriptionFetcher implements StripeSubscriptionFetcher {

    private final StripeConfig stripeConfig;

    public DefaultStripeSubscriptionFetcher(StripeConfig stripeConfig) {
        this.stripeConfig = stripeConfig;
    }

    @Override
    public Subscription fetch(String subscriptionId) throws StripeException {
        stripeConfig.ensureConfigured();
        return Subscription.retrieve(subscriptionId);
    }
}
