package com.urlshort.dto.link;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO containing short link details.
 * Implemented as an immutable Java record.
 */
@Schema(description = "Response object containing short link details")
public record ShortLinkResponse(

        @Schema(description = "Unique identifier of the short link", example = "12345")
        Long id,

        @Schema(description = "The short code/slug", example = "abc123")
        @JsonProperty("short_code")
        String shortCode,

        @Schema(description = "The complete shortened URL", example = "https://short.ly/abc123")
        @JsonProperty("short_url")
        String shortUrl,

        @Schema(description = "The original long URL", example = "https://www.example.com/very/long/url")
        @JsonProperty("original_url")
        String originalUrl,

        @Schema(description = "Normalized version of the original URL", example = "https://www.example.com/very/long/url")
        @JsonProperty("normalized_url")
        String normalizedUrl,

        @Schema(description = "Timestamp when the link was created", example = "2025-11-18T10:30:00")
        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp when the link expires (null if no expiration)", example = "2025-12-31T23:59:59")
        @JsonProperty("expires_at")
        LocalDateTime expiresAt,

        @Schema(description = "Total number of clicks", example = "42")
        @JsonProperty("click_count")
        Long clickCount,

        @Schema(description = "Whether the link is currently active", example = "true")
        @JsonProperty("is_active")
        Boolean isActive,

        @Schema(description = "Tags associated with the link", example = "[\"marketing\", \"campaign-2024\"]")
        Set<String> tags
) implements Serializable {
    /**
     * Builder pattern for ShortLinkResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String shortCode;
        private String shortUrl;
        private String originalUrl;
        private String normalizedUrl;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private Long clickCount;
        private Boolean isActive;
        private Set<String> tags;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder shortCode(String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public Builder shortUrl(String shortUrl) {
            this.shortUrl = shortUrl;
            return this;
        }

        public Builder originalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        public Builder normalizedUrl(String normalizedUrl) {
            this.normalizedUrl = normalizedUrl;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder clickCount(Long clickCount) {
            this.clickCount = clickCount;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        public ShortLinkResponse build() {
            return new ShortLinkResponse(
                    id, shortCode, shortUrl, originalUrl, normalizedUrl,
                    createdAt, expiresAt, clickCount, isActive, tags
            );
        }
    }
}
