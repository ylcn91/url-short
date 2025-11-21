package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for webhook delivery result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Webhook delivery result")
public class WebhookDeliveryResponse {

    @Schema(description = "Whether delivery was successful", example = "true")
    private Boolean success;

    @Schema(description = "HTTP status code", example = "200")
    @JsonProperty("status_code")
    private Integer statusCode;

    @Schema(description = "Delivery result message")
    private String message;
}
