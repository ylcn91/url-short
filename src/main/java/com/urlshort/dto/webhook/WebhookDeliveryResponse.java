package com.urlshort.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for webhook delivery result.
 */
@Schema(description = "Webhook delivery result")
public record WebhookDeliveryResponse(
        @Schema(description = "Whether delivery was successful", example = "true")
        Boolean success,

        @Schema(description = "HTTP status code", example = "200")
        @JsonProperty("status_code")
        Integer statusCode,

        @Schema(description = "Delivery result message")
        String message
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Boolean success;
        private Integer statusCode;
        private String message;

        public Builder success(Boolean success) { this.success = success; return this; }
        public Builder statusCode(Integer statusCode) { this.statusCode = statusCode; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public WebhookDeliveryResponse build() { return new WebhookDeliveryResponse(success, statusCode, message); }
    }
}
