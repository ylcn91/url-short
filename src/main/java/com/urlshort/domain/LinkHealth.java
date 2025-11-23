package com.urlshort.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreatedDate;
import org.hibernate.annotations.UpdatedDate;

import java.time.LocalDateTime;

/**
 * Link health monitoring record.
 * Tracks the status of destination URLs.
 */
@Entity
@Table(name = "link_health", indexes = {
    @Index(name = "idx_health_link", columnList = "short_link_id"),
    @Index(name = "idx_health_status", columnList = "status"),
    @Index(name = "idx_health_check_time", columnList = "last_checked_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkHealth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The short link being monitored.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_link_id", nullable = false, unique = true)
    private ShortLink shortLink;

    /**
     * Current health status.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HealthStatus status = HealthStatus.UNKNOWN;

    /**
     * Last HTTP status code received.
     */
    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    /**
     * Last response time in milliseconds.
     */
    @Column(name = "last_response_time_ms")
    private Long lastResponseTimeMs;

    /**
     * Last error message (if any).
     */
    @Column(name = "last_error", length = 500)
    private String lastError;

    /**
     * Number of consecutive failures.
     */
    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private Integer consecutiveFailures = 0;

    /**
     * Total number of checks performed.
     */
    @Column(name = "check_count", nullable = false)
    @Builder.Default
    private Long checkCount = 0L;

    /**
     * Number of successful checks.
     */
    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private Long successCount = 0L;

    /**
     * When the link was last checked.
     */
    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    /**
     * When the health record was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the health record was last updated.
     */
    @UpdatedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Health status enum.
     */
    public enum HealthStatus {
        /**
         * Health status is unknown (never checked).
         */
        UNKNOWN,

        /**
         * Link is healthy (200-299 response).
         */
        HEALTHY,

        /**
         * Link is degraded (300-399 or slow response).
         */
        DEGRADED,

        /**
         * Link is unhealthy (400-499 or timeouts).
         */
        UNHEALTHY,

        /**
         * Link is down (500+ or connection errors).
         */
        DOWN
    }

    /**
     * Record a successful health check.
     */
    public void recordSuccess(int statusCode, long responseTimeMs) {
        checkCount++;
        successCount++;
        consecutiveFailures = 0;
        lastStatusCode = statusCode;
        lastResponseTimeMs = responseTimeMs;
        lastError = null;
        lastCheckedAt = LocalDateTime.now();

        // Determine status
        if (statusCode >= 200 && statusCode < 300) {
            status = responseTimeMs > 3000 ? HealthStatus.DEGRADED : HealthStatus.HEALTHY;
        } else if (statusCode >= 300 && statusCode < 400) {
            status = HealthStatus.DEGRADED;
        }
    }

    /**
     * Record a failed health check.
     */
    public void recordFailure(int statusCode, String error) {
        checkCount++;
        consecutiveFailures++;
        lastStatusCode = statusCode;
        lastError = error;
        lastCheckedAt = LocalDateTime.now();

        // Determine status based on status code
        if (statusCode >= 500) {
            status = HealthStatus.DOWN;
        } else if (statusCode >= 400) {
            status = consecutiveFailures >= 3 ? HealthStatus.DOWN : HealthStatus.UNHEALTHY;
        } else {
            status = HealthStatus.UNHEALTHY;
        }
    }

    /**
     * Get uptime percentage.
     */
    public double getUptimePercentage() {
        if (checkCount == 0) {
            return 100.0;
        }
        return (double) successCount / checkCount * 100;
    }

    /**
     * Check if link needs immediate attention.
     */
    public boolean needsAttention() {
        return status == HealthStatus.DOWN ||
               status == HealthStatus.UNHEALTHY ||
               consecutiveFailures >= 3;
    }
}
