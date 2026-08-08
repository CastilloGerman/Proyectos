package com.appgestion.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    /** TTL explícito para rankings de solo lectura; no cache indefinida. */
    static final Duration MATERIALES_TOP_USADOS_TTL = Duration.ofMinutes(5);

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CacheNames.MATERIALES_TOP_USADOS);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(MATERIALES_TOP_USADOS_TTL)
                .maximumSize(10_000));
        return manager;
    }
}
