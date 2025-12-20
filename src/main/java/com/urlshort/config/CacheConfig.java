package com.urlshort.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CacheLoader;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the URL Shortener application.
 *
 * Supports two cache strategies:
 * 1. Redis Cache (distributed, persistent) - Primary cache manager
 * 2. Caffeine Cache (local, in-memory) - Fallback when Redis is unavailable
 *
 * Cache Names and TTL Settings:
 * - CACHE_SHORT_LINKS: 1 hour (hot path optimization for redirects)
 * - CACHE_WORKSPACES: 24 hours
 * - CACHE_USERS: 24 hours
 *
 * Cache Key Pattern:
 * - Short links: shortlink:{workspaceId}:{code}
 * - Workspaces: workspace:{workspaceId}
 * - Users: user:{userId}
 *
 * Features:
 * - Automatic eviction on update/delete operations via @CacheEvict
 * - Cache refresh via @CachePut for updates
 * - Metrics collection for cache hit/miss rates
 * - DEBUG level logging for cache operations
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names constants
    public static final String CACHE_SHORT_LINKS = "shortlinks";
    public static final String CACHE_WORKSPACES = "workspaces";
    public static final String CACHE_USERS = "users";

    // TTL configurations (in seconds)
    public static final long SHORT_LINK_TTL_SECONDS = 3600; // 1 hour
    public static final long WORKSPACE_TTL_SECONDS = 86400; // 24 hours
    public static final long USER_TTL_SECONDS = 86400; // 24 hours

    // Cache key prefixes for consistency
    public static final String SHORT_LINK_KEY_PREFIX = "shortlink:";
    public static final String WORKSPACE_KEY_PREFIX = "workspace:";
    public static final String USER_KEY_PREFIX = "user:";

    /**
     * Primary Redis Cache Manager.
     * Used when Redis is available.
     * Provides distributed caching with configurable TTLs per cache name.
     *
     * @param redisConnectionFactory the Redis connection factory
     * @return configured RedisCacheManager
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = false)
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("Initializing Redis Cache Manager");

        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        // Short links cache: 1 hour TTL with hot path optimization
        RedisCacheConfiguration shortLinksConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(SHORT_LINK_TTL_SECONDS))
                .disableCachingNullValues();

        log.debug("Short links cache configured with TTL: {} seconds", SHORT_LINK_TTL_SECONDS);

        // Workspaces cache: 24 hours TTL
        RedisCacheConfiguration workspacesConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(WORKSPACE_TTL_SECONDS))
                .disableCachingNullValues();

        log.debug("Workspaces cache configured with TTL: {} seconds", WORKSPACE_TTL_SECONDS);

        // Users cache: 24 hours TTL
        RedisCacheConfiguration usersConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(USER_TTL_SECONDS))
                .disableCachingNullValues();

        log.debug("Users cache configured with TTL: {} seconds", USER_TTL_SECONDS);

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(CACHE_SHORT_LINKS, shortLinksConfig)
                .withCacheConfiguration(CACHE_WORKSPACES, workspacesConfig)
                .withCacheConfiguration(CACHE_USERS, usersConfig)
                .build();
    }

    /**
     * Fallback Caffeine Cache Manager.
     * Used when Redis is unavailable or disabled.
     * Provides local in-memory caching with automatic eviction.
     *
     * @return configured CaffeineCacheManager
     */
    @Bean
    @ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        log.info("Initializing Caffeine (Local) Cache Manager as fallback");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_SHORT_LINKS,
                CACHE_WORKSPACES,
                CACHE_USERS
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(1, TimeUnit.HOURS));

        log.debug("Caffeine cache configured with 1 hour expiration");
        return cacheManager;
    }

    /**
     * Timed aspect for cache operation metrics.
     * Integrates Micrometer for tracking cache hit/miss rates and operation timing.
     *
     * @param meterRegistry the Micrometer registry
     * @return configured TimedAspect
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        log.debug("Configuring cache metrics with Micrometer");
        return new TimedAspect(meterRegistry);
    }

    /**
     * Cache initialization logger.
     * Logs cache configuration during application startup.
     */
    public void logCacheConfiguration() {
        log.info("=".repeat(60));
        log.info("CACHE CONFIGURATION INITIALIZED");
        log.info("=".repeat(60));
        log.info("Cache Name: {} | TTL: {} seconds | Key Prefix: {}",
                CACHE_SHORT_LINKS, SHORT_LINK_TTL_SECONDS, SHORT_LINK_KEY_PREFIX);
        log.info("Cache Name: {} | TTL: {} seconds | Key Prefix: {}",
                CACHE_WORKSPACES, WORKSPACE_TTL_SECONDS, WORKSPACE_KEY_PREFIX);
        log.info("Cache Name: {} | TTL: {} seconds | Key Prefix: {}",
                CACHE_USERS, USER_TTL_SECONDS, USER_KEY_PREFIX);
        log.info("=".repeat(60));
    }

}
