package com.appgestion.api.scheduler;

import com.appgestion.api.service.SubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job programado que expira los trials vencidos.
 * Delega en SubscriptionService (principio de responsabilidad única).
 */
@Component
public class TrialExpirationJob {

    private final SubscriptionService subscriptionService;

    public TrialExpirationJob(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void expireTrials() {
        subscriptionService.expireTrials();
    }
}
