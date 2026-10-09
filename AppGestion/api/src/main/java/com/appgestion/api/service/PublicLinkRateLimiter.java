package com.appgestion.api.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PublicLinkRateLimiter {
    private final Cache<String, AtomicInteger> requests = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1)).maximumSize(100_000).build();
    private final int perIp;
    private final int perToken;

    public PublicLinkRateLimiter(@Value("${app.public-links.rate-limit-per-ip:120}") int perIp,
                                 @Value("${app.public-links.rate-limit-per-token:60}") int perToken) {
        this.perIp = perIp;
        this.perToken = perToken;
    }

    public boolean allow(String remoteAddress, String tokenHash) {
        if (!consume("token:" + tokenHash, perToken)) return false;
        return consume("ip:" + remoteAddress, perIp);
    }

    private boolean consume(String key, int limit) {
        return requests.get(key, ignored -> new AtomicInteger()).incrementAndGet() <= limit;
    }
}
