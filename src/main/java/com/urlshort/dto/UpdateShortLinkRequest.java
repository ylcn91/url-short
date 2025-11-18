package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for updating an existing short link.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for updating an existing short link")
public class UpdateShortLinkRequest {

    @org.hibernate.validator.constraints.URL(message = "Must be a valid URL")
    @Schema(description = "The new original URL", example = "https://www.example.com/updated/url")
    @JsonProperty("original_url")
    private String originalUrl;

    @Future(message = "Expiration date must be in the future")
    @Schema(description = "New expiration date/time for the short link",
            example = "2025-12-31T23:59:59")
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @Min(value = 1, message = "Max clicks must be at least 1")
    @Schema(description = "New maximum number of clicks allowed", example = "5000")
    @JsonProperty("max_clicks")
    private Integer maxClicks;

    @Schema(description = "Whether the link is active", example = "true")
    @JsonProperty("is_active")
    private Boolean isActive;
}
