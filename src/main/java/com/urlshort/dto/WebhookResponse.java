package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for webhook.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Webhook response")
public class WebhookResponse {

    @Schema(description = "Webhook ID", example = "1")
    private Long id;

    @Schema(description = "Workspace ID", example = "100")
    @JsonProperty("workspace_id")
    private Long workspaceId;

    @Schema(description = "Webhook name", example = "Slack Notifications")
    private String name;

    @Schema(description = "Webhook URL", example = "https://hooks.slack.com/...")
    private String url;

    @Schema(description = "Webhook secret for signature verification")
    private String secret;

    @Schema(description = "Subscribed event types")
    private Set<String> events;

    @Schema(description = "Whether webhook is active", example = "true")
    @JsonProperty("is_active")
    private Boolean isActive;

    @Schema(description = "Total delivery attempts", example = "150")
    @JsonProperty("delivery_count")
    private Long deliveryCount;

    @Schema(description = "Failed delivery attempts", example = "5")
    @JsonProperty("failure_count")
    private Long failureCount;

    @Schema(description = "Delivery success rate percentage", example = "96.67")
    @JsonProperty("success_rate")
    private Double successRate;

    @Schema(description = "Last delivery status", example = "SUCCESS")
    @JsonProperty("last_status")
    private String lastStatus;

    @Schema(description = "Last delivery timestamp")
    @JsonProperty("last_delivery_at")
    private LocalDateTime lastDeliveryAt;

    @Schema(description = "When webhook was created")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
