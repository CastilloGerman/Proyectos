package com.appgestion.api.unit.service;

import com.appgestion.api.config.GeminiProperties;
import com.appgestion.api.exception.AiServiceException;
import com.appgestion.api.service.AiDailyRequestRateLimiter;
import com.appgestion.api.service.AiRequestRateLimiter;
import com.appgestion.api.service.AiRequestQuotaPermit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class AiDailyRequestRateLimiterTest {

    @Test
    void enforcesConfiguredPerUserDailyQuota() {
        GeminiProperties properties = new GeminiProperties();
        properties.setPresupuestoRequestsPerDay(1);
        AiDailyRequestRateLimiter limiter = new AiDailyRequestRateLimiter(properties);

        limiter.checkAndRecord(1L);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                assertThrows(AiServiceException.class, () -> limiter.checkAndRecord(1L)).getStatus());
        assertDoesNotThrow(() -> limiter.checkAndRecord(2L));
    }

    @Test
    void concurrentDailyReservationsNeverExceedTheConfiguredLimit() throws Exception {
        GeminiProperties properties = new GeminiProperties();
        properties.setPresupuestoRequestsPerDay(3);
        AiDailyRequestRateLimiter limiter = new AiDailyRequestRateLimiter(properties);

        assertEquals(3, concurrentSuccessfulReservations(() -> limiter.reserve(101L)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                assertThrows(AiServiceException.class, () -> limiter.reserve(101L)).getStatus());
    }

    @Test
    void concurrentHourlyReservationsNeverExceedTheConfiguredLimit() throws Exception {
        GeminiProperties properties = new GeminiProperties();
        properties.setRequestsPerHour(3);
        AiRequestRateLimiter limiter = new AiRequestRateLimiter(properties);

        assertEquals(3, concurrentSuccessfulReservations(() -> limiter.reserve(101L)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,
                assertThrows(AiServiceException.class, () -> limiter.reserve(101L)).getStatus());
    }

    private static int concurrentSuccessfulReservations(ReservationFactory factory) throws Exception {
        int workers = 12;
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(workers)) {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try (AiRequestQuotaPermit permit = factory.reserve()) {
                        permit.commit();
                        return true;
                    } catch (AiServiceException ex) {
                        return false;
                    }
                }));
            }
            ready.await();
            start.countDown();
            int successful = 0;
            for (var future : futures) if (future.get()) successful++;
            return successful;
        }
    }

    @FunctionalInterface
    private interface ReservationFactory {
        AiRequestQuotaPermit reserve();
    }
}
