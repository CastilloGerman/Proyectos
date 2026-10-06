package com.appgestion.api.unit.service;

import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.exception.AiServiceException;
import com.appgestion.api.service.AiProviderAttemptRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiProviderAttemptRateLimiterTest {

    @Test
    void defaultsToSixtyProviderAttemptsPerHour() {
        assertEquals(60, new GeminiProperties().getMaxProviderAttemptsPerHour());
    }

    @Test
    void atomicallyLimitsConcurrentAttemptsPerUser() throws Exception {
        GeminiProperties properties = new GeminiProperties();
        properties.setMaxProviderAttemptsPerHour(7);
        AiProviderAttemptRateLimiter limiter = new AiProviderAttemptRateLimiter(properties);
        int workers = 32;
        var ready = new CountDownLatch(workers);
        var start = new CountDownLatch(1);
        var accepted = new AtomicInteger();
        var rejected = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(workers)) {
            for (int i = 0; i < workers; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        limiter.checkAndRecord(42L);
                        accepted.incrementAndGet();
                    } catch (AiServiceException ex) {
                        if (ex.getStatus() == HttpStatus.TOO_MANY_REQUESTS) rejected.incrementAndGet();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            ready.await();
            start.countDown();
        }

        assertEquals(7, accepted.get());
        assertEquals(workers - 7, rejected.get());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                assertThrows(AiServiceException.class, () -> limiter.checkAndRecord(42L)).getStatus());
    }
}
