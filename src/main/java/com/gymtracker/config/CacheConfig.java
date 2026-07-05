package com.gymtracker.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring cache support for dashboard and statistics aggregations.
 * <p>
 * Uses a bounded, time-expiring Caffeine cache instead of the default
 * {@code ConcurrentMapCacheManager} Spring Boot would otherwise fall back to,
 * which never expires or bounds entries and can grow unboundedly over the
 * life of the application.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DASHBOARD_CACHE = "dashboards";
    public static final String STATISTICS_CACHE = "statistics";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DASHBOARD_CACHE, STATISTICS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(10, TimeUnit.MINUTES));
        cacheManager.setCacheNames(List.of(DASHBOARD_CACHE, STATISTICS_CACHE));
        return cacheManager;
    }
}
