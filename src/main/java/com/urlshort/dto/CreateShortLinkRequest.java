package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Request DTO for creating a new short link.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating a new short link")
public class CreateShortLinkRequest {

    @NotBlank(message = "Original URL is required")
    @Schema(description = "The original URL to be shortened", example = "https://www.example.com/very/long/url")
    @org.hibernate.validator.constraints.URL(message = "Must be a valid URL")
    @JsonProperty("original_url")
    private String originalUrl;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,20}$",
             message = "Custom code must be 3-20 characters long and contain only alphanumeric characters, hyphens, or underscores")
    @Schema(description = "Optional custom short code (3-20 alphanumeric characters, hyphens, or underscores)",
            example = "my-custom-code")
    @JsonProperty("custom_code")
    private String customCode;

    @Future(message = "Expiration date must be in the future")
    @Schema(description = "Optional expiration date/time for the short link",
            example = "2025-12-31T23:59:59")
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;

    @Min(value = 1, message = "Max clicks must be at least 1")
    @Schema(description = "Optional maximum number of clicks allowed before the link becomes inactive",
            example = "1000")
    @JsonProperty("max_clicks")
    private Integer maxClicks;

    @Schema(description = "Optional tags for categorizing the short link",
            example = "[\"marketing\", \"campaign-2024\"]")
    private Set<String> tags;
}
