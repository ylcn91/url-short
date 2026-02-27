package com.urlshort.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting configuration using Bucket4j.
 * Provides token bucket-based rate limiting for different API endpoints.
 * Rate limits:
 * - Public redirect endpoints: 100 requests per minute per IP
 * - Management API (authenticated): 1000 requests per minute per user
 * - Link creation: 50 requests per minute per user
 */
@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitConfig {

    /**
     * Cache for storing rate limit buckets per IP/user
     */
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Rate limit for public redirect endpoints (per IP)
     * Default: 100 requests per minute
     */
    private int redirectRequestsPerMinute = 100;

    /**
     * Rate limit for management API endpoints (per user)
     * Default: 1000 requests per minute
     */
    private int managementRequestsPerMinute = 1000;

    /**
     * Rate limit for link creation (per user)
     * Default: 50 requests per minute
     */
    private int creationRequestsPerMinute = 50;

    /**
     * Enable/disable rate limiting
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Resolves or creates a bucket for redirect endpoint rate limiting.
     * Uses IP-based rate limiting.
     *
     * @param key the unique identifier (typically IP address)
     * @return the rate limiting bucket
     */
    public Bucket resolveRedirectBucket(String key) {
        return cache.computeIfAbsent(key, k -> createRedirectBucket());
    }

    /**
     * Resolves or creates a bucket for management API rate limiting.
     * Uses user-based rate limiting.
     *
     * @param key the unique identifier (typically user ID or API key)
     * @return the rate limiting bucket
     */
    public Bucket resolveManagementBucket(String key) {
        return cache.computeIfAbsent(key, k -> createManagementBucket());
    }

    /**
     * Resolves or creates a bucket for link creation rate limiting.
     * Uses user-based rate limiting with stricter limits.
     *
     * @param key the unique identifier (typically user ID)
     * @return the rate limiting bucket
     */
    public Bucket resolveCreationBucket(String key) {
        return cache.computeIfAbsent(key, k -> createCreationBucket());
    }

    /**
     * Creates a new bucket for redirect endpoints.
     * Token bucket with capacity equal to requests per minute, refilling at constant rate.
     *
     * @return new Bucket instance
     */
    private Bucket createRedirectBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(redirectRequestsPerMinute)
            .refillIntervally(redirectRequestsPerMinute, Duration.ofMinutes(1))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Creates a new bucket for management API endpoints.
     * Higher capacity for authenticated users.
     *
     * @return new Bucket instance
     */
    private Bucket createManagementBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(managementRequestsPerMinute)
            .refillIntervally(managementRequestsPerMinute, Duration.ofMinutes(1))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Creates a new bucket for link creation endpoints.
     * Lower capacity to prevent abuse.
     *
     * @return new Bucket instance
     */
    private Bucket createCreationBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(creationRequestsPerMinute)
            .refillIntervally(creationRequestsPerMinute, Duration.ofMinutes(1))
            .build();
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Clears the rate limit cache.
     * Useful for testing or administrative operations.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Gets the size of the rate limit cache.
     *
     * @return number of cached buckets
     */
    public int getCacheSize() {
        return cache.size();
    }

}
