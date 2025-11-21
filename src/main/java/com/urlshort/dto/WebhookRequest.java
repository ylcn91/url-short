package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating/updating a webhook.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for webhook configuration")
public class WebhookRequest {

    @NotBlank(message = "Webhook name is required")
    @Schema(description = "Webhook name", example = "Slack Notifications")
    private String name;

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https://.*", message = "Webhook URL must use HTTPS")
    @Schema(description = "Webhook endpoint URL", example = "https://hooks.slack.com/services/...")
    private String url;

    @NotEmpty(message = "At least one event is required")
    @Schema(description = "Event types to subscribe to",
            example = "[\"link.created\", \"link.clicked\", \"link.expired\"]")
    private List<String> events;

    @Schema(description = "Whether webhook is active", example = "true")
    @JsonProperty("is_active")
    private Boolean isActive;
}
