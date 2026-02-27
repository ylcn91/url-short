package com.urlshort.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for webhook.
 */
@Schema(description = "Webhook response")
public record WebhookResponse(
        @Schema(description = "Webhook ID", example = "1")
        Long id,

        @Schema(description = "Workspace ID", example = "100")
        @JsonProperty("workspace_id")
        Long workspaceId,

        @Schema(description = "Webhook name", example = "Slack Notifications")
        String name,

        @Schema(description = "Webhook URL", example = "https://hooks.slack.com/...")
        String url,

        @Schema(description = "Webhook secret for signature verification")
        String secret,

        @Schema(description = "Subscribed event types")
        Set<String> events,

        @Schema(description = "Whether webhook is active", example = "true")
        @JsonProperty("is_active")
        Boolean isActive,

        @Schema(description = "Total delivery attempts", example = "150")
        @JsonProperty("delivery_count")
        Long deliveryCount,

        @Schema(description = "Failed delivery attempts", example = "5")
        @JsonProperty("failure_count")
        Long failureCount,

        @Schema(description = "Delivery success rate percentage", example = "96.67")
        @JsonProperty("success_rate")
        Double successRate,

        @Schema(description = "Last delivery status", example = "SUCCESS")
        @JsonProperty("last_status")
        String lastStatus,

        @Schema(description = "Last delivery timestamp")
        @JsonProperty("last_delivery_at")
        LocalDateTime lastDeliveryAt,

        @Schema(description = "When webhook was created")
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long workspaceId;
        private String name;
        private String url;
        private String secret;
        private Set<String> events;
        private Boolean isActive;
        private Long deliveryCount;
        private Long failureCount;
        private Double successRate;
        private String lastStatus;
        private LocalDateTime lastDeliveryAt;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder workspaceId(Long workspaceId) { this.workspaceId = workspaceId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder secret(String secret) { this.secret = secret; return this; }
        public Builder events(Set<String> events) { this.events = events; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder deliveryCount(Long deliveryCount) { this.deliveryCount = deliveryCount; return this; }
        public Builder failureCount(Long failureCount) { this.failureCount = failureCount; return this; }
        public Builder successRate(Double successRate) { this.successRate = successRate; return this; }
        public Builder lastStatus(String lastStatus) { this.lastStatus = lastStatus; return this; }
        public Builder lastDeliveryAt(LocalDateTime lastDeliveryAt) { this.lastDeliveryAt = lastDeliveryAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public WebhookResponse build() { return new WebhookResponse(id, workspaceId, name, url, secret, events, isActive, deliveryCount, failureCount, successRate, lastStatus, lastDeliveryAt, createdAt); }
    }
}
