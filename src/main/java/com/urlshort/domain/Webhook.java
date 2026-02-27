package com.urlshort.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Webhook configuration for workspace events.
 * Allows workspaces to receive real-time notifications.
 * Events:
 * - link.created
 * - link.clicked
 * - link.expired
 * - link.disabled
 */
@Entity
@Table(name = "webhooks", indexes = {
    @Index(name = "idx_webhook_workspace", columnList = "workspace_id"),
    @Index(name = "idx_webhook_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The workspace that owns this webhook.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /**
     * Webhook name/description.
     */
    @NotBlank(message = "Name cannot be empty")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Webhook URL endpoint.
     */
    @NotBlank(message = "URL cannot be empty")
    @Pattern(regexp = "^https://.*", message = "Webhook URL must use HTTPS")
    @Column(nullable = false, length = 2048)
    private String url;

    /**
     * Secret key for webhook signature verification.
     * Used to sign webhook payloads with HMAC-SHA256.
     */
    @Column(nullable = false, length = 64)
    private String secret;

    /**
     * Events this webhook subscribes to.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "webhook_events", joinColumns = @JoinColumn(name = "webhook_id"))
    @Column(name = "event_type", length = 50)
    @Builder.Default
    private Set<String> events = new HashSet<>();

    /**
     * Whether the webhook is active.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Number of delivery attempts.
     */
    @Column(name = "delivery_count", nullable = false)
    @Builder.Default
    private Long deliveryCount = 0L;

    /**
     * Number of failed deliveries.
     */
    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private Long failureCount = 0L;

    /**
     * Last delivery status.
     */
    @Column(name = "last_status", length = 20)
    private String lastStatus;

    /**
     * Last delivery attempt time.
     */
    @Column(name = "last_delivery_at")
    private LocalDateTime lastDeliveryAt;

    /**
     * When the webhook was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the webhook was last updated.
     */
    @LastModifiedDate
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
     * Record a successful delivery.
     */
    public void recordSuccess() {
        deliveryCount++;
        lastStatus = "SUCCESS";
        lastDeliveryAt = LocalDateTime.now();
    }

    /**
     * Record a failed delivery.
     */
    public void recordFailure() {
        deliveryCount++;
        failureCount++;
        lastStatus = "FAILED";
        lastDeliveryAt = LocalDateTime.now();

        // Disable webhook after 10 consecutive failures
        if (failureCount >= 10) {
            isActive = false;
        }
    }

    /**
     * Get delivery success rate.
     */
    public double getSuccessRate() {
        if (deliveryCount == 0) {
            return 100.0;
        }
        return (double) (deliveryCount - failureCount) / deliveryCount * 100;
    }
}
