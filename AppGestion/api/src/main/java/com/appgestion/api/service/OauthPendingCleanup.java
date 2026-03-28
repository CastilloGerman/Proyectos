package com.appgestion.api.service;

import com.appgestion.api.repository.OauthPendingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class OauthPendingCleanup {

    private final OauthPendingRepository oauthPendingRepository;

    public OauthPendingCleanup(OauthPendingRepository oauthPendingRepository) {
        this.oauthPendingRepository = oauthPendingRepository;
    }

    @Scheduled(fixedDelayString = "${app.email.oauth-pending-cleanup-ms:600000}")
    @Transactional
    public void deleteExpired() {
        oauthPendingRepository.deleteExpired(Instant.now());
    }
}
