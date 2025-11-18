package com.urlshort.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Cache metrics collection and monitoring component.
 *
 * Tracks:
 * - Cache hits and misses per cache name
 * - Cache operation timing (get, put, evict)
 * - Cache eviction counts
 * - Overall cache performance statistics
 *
 * Metrics are exposed via Micrometer for Prometheus scraping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheMetrics {

    private final MeterRegistry meterRegistry;

    // Counters
    private Counter shortLinkCacheHits;
    private Counter shortLinkCacheMisses;
    private Counter workspaceCacheHits;
    private Counter workspaceCacheMisses;
    private Counter userCacheHits;
    private Counter userCacheMisses;
    private Counter shortLinkCacheEvictions;
    private Counter workspaceCacheEvictions;
    private Counter userCacheEvictions;

    // Timers
    private Timer shortLinkCacheGetTimer;
    private Timer shortLinkCachePutTimer;
    private Timer workspaceCacheGetTimer;
    private Timer workspaceCachePutTimer;
    private Timer userCacheGetTimer;
    private Timer userCachePutTimer;

    /**
     * Initialize cache metrics during component construction.
     */
    public void initializeMetrics() {
        log.debug("Initializing cache metrics");

        // Short link cache metrics
        shortLinkCacheHits = Counter.builder("cache.hits")
                .description("Short link cache hits")
                .tag("cache", CacheConfig.CACHE_SHORT_LINKS)
                .register(meterRegistry);

        shortLinkCacheMisses = Counter.builder("cache.misses")
                .description("Short link cache misses")
                .tag("cache", CacheConfig.CACHE_SHORT_LINKS)
                .register(meterRegistry);

        shortLinkCacheEvictions = Counter.builder("cache.evictions")
                .description("Short link cache evictions")
                .tag("cache", CacheConfig.CACHE_SHORT_LINKS)
                .register(meterRegistry);

        shortLinkCacheGetTimer = Timer.builder("cache.get.time")
                .description("Short link cache get operation time")
                .tag("cache", CacheConfig.CACHE_SHORT_LINKS)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        shortLinkCachePutTimer = Timer.builder("cache.put.time")
                .description("Short link cache put operation time")
                .tag("cache", CacheConfig.CACHE_SHORT_LINKS)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // Workspace cache metrics
        workspaceCacheHits = Counter.builder("cache.hits")
                .description("Workspace cache hits")
                .tag("cache", CacheConfig.CACHE_WORKSPACES)
                .register(meterRegistry);

        workspaceCacheMisses = Counter.builder("cache.misses")
                .description("Workspace cache misses")
                .tag("cache", CacheConfig.CACHE_WORKSPACES)
                .register(meterRegistry);

        workspaceCacheEvictions = Counter.builder("cache.evictions")
                .description("Workspace cache evictions")
                .tag("cache", CacheConfig.CACHE_WORKSPACES)
                .register(meterRegistry);

        workspaceCacheGetTimer = Timer.builder("cache.get.time")
                .description("Workspace cache get operation time")
                .tag("cache", CacheConfig.CACHE_WORKSPACES)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        workspaceCachePutTimer = Timer.builder("cache.put.time")
                .description("Workspace cache put operation time")
                .tag("cache", CacheConfig.CACHE_WORKSPACES)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        // User cache metrics
        userCacheHits = Counter.builder("cache.hits")
                .description("User cache hits")
                .tag("cache", CacheConfig.CACHE_USERS)
                .register(meterRegistry);

        userCacheMisses = Counter.builder("cache.misses")
                .description("User cache misses")
                .tag("cache", CacheConfig.CACHE_USERS)
                .register(meterRegistry);

        userCacheEvictions = Counter.builder("cache.evictions")
                .description("User cache evictions")
                .tag("cache", CacheConfig.CACHE_USERS)
                .register(meterRegistry);

        userCacheGetTimer = Timer.builder("cache.get.time")
                .description("User cache get operation time")
                .tag("cache", CacheConfig.CACHE_USERS)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        userCachePutTimer = Timer.builder("cache.put.time")
                .description("User cache put operation time")
                .tag("cache", CacheConfig.CACHE_USERS)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);

        log.info("Cache metrics initialized successfully");
    }

    // Short Link Cache Methods
    public void recordShortLinkCacheHit() {
        shortLinkCacheHits.increment();
        log.debug("Short link cache hit recorded");
    }

    public void recordShortLinkCacheMiss() {
        shortLinkCacheMisses.increment();
        log.debug("Short link cache miss recorded");
    }

    public void recordShortLinkCacheEviction() {
        shortLinkCacheEvictions.increment();
        log.debug("Short link cache eviction recorded");
    }

    public <T> T recordShortLinkCacheGet(java.util.concurrent.Callable<T> callable) throws Exception {
        return shortLinkCacheGetTimer.recordCallable(callable);
    }

    public <T> T recordShortLinkCachePut(java.util.concurrent.Callable<T> callable) throws Exception {
        return shortLinkCachePutTimer.recordCallable(callable);
    }

    // Workspace Cache Methods
    public void recordWorkspaceCacheHit() {
        workspaceCacheHits.increment();
        log.debug("Workspace cache hit recorded");
    }

    public void recordWorkspaceCacheMiss() {
        workspaceCacheMisses.increment();
        log.debug("Workspace cache miss recorded");
    }

    public void recordWorkspaceCacheEviction() {
        workspaceCacheEvictions.increment();
        log.debug("Workspace cache eviction recorded");
    }

    public <T> T recordWorkspaceCacheGet(java.util.concurrent.Callable<T> callable) throws Exception {
        return workspaceCacheGetTimer.recordCallable(callable);
    }

    public <T> T recordWorkspaceCachePut(java.util.concurrent.Callable<T> callable) throws Exception {
        return workspaceCachePutTimer.recordCallable(callable);
    }

    // User Cache Methods
    public void recordUserCacheHit() {
        userCacheHits.increment();
        log.debug("User cache hit recorded");
    }

    public void recordUserCacheMiss() {
        userCacheMisses.increment();
        log.debug("User cache miss recorded");
    }

    public void recordUserCacheEviction() {
        userCacheEvictions.increment();
        log.debug("User cache eviction recorded");
    }

    public <T> T recordUserCacheGet(java.util.concurrent.Callable<T> callable) throws Exception {
        return userCacheGetTimer.recordCallable(callable);
    }

    public <T> T recordUserCachePut(java.util.concurrent.Callable<T> callable) throws Exception {
        return userCachePutTimer.recordCallable(callable);
    }

    /**
     * Log cache statistics at INFO level.
     */
    public void logCacheStatistics() {
        log.info("=".repeat(80));
        log.info("CACHE STATISTICS");
        log.info("=".repeat(80));
        log.info("Short Link Cache | Hits: {} | Misses: {} | Evictions: {}",
                shortLinkCacheHits.count(),
                shortLinkCacheMisses.count(),
                shortLinkCacheEvictions.count());
        log.info("Workspace Cache | Hits: {} | Misses: {} | Evictions: {}",
                workspaceCacheHits.count(),
                workspaceCacheMisses.count(),
                workspaceCacheEvictions.count());
        log.info("User Cache | Hits: {} | Misses: {} | Evictions: {}",
                userCacheHits.count(),
                userCacheMisses.count(),
                userCacheEvictions.count());
        log.info("=".repeat(80));
    }

}
