package com.urlshort.dto.variant;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for A/B test variant.
 */
@Schema(description = "A/B test variant response")
public record LinkVariantResponse(
        @Schema(description = "Variant ID", example = "1")
        Long id,

        @Schema(description = "Short link ID", example = "100")
        @JsonProperty("short_link_id")
        Long shortLinkId,

        @Schema(description = "Variant name", example = "Control")
        String name,

        @Schema(description = "Destination URL", example = "https://example.com/page-a")
        @JsonProperty("destination_url")
        String destinationUrl,

        @Schema(description = "Traffic weight percentage", example = "50")
        Integer weight,

        @Schema(description = "Total clicks for this variant", example = "245")
        @JsonProperty("click_count")
        Long clickCount,

        @Schema(description = "Total conversions for this variant", example = "23")
        @JsonProperty("conversion_count")
        Long conversionCount,

        @Schema(description = "Conversion rate percentage", example = "9.39")
        @JsonProperty("conversion_rate")
        Double conversionRate,

        @Schema(description = "Whether variant is active", example = "true")
        @JsonProperty("is_active")
        Boolean isActive,

        @Schema(description = "When variant was created")
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long shortLinkId;
        private String name;
        private String destinationUrl;
        private Integer weight;
        private Long clickCount;
        private Long conversionCount;
        private Double conversionRate;
        private Boolean isActive;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder shortLinkId(Long shortLinkId) { this.shortLinkId = shortLinkId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder destinationUrl(String destinationUrl) { this.destinationUrl = destinationUrl; return this; }
        public Builder weight(Integer weight) { this.weight = weight; return this; }
        public Builder clickCount(Long clickCount) { this.clickCount = clickCount; return this; }
        public Builder conversionCount(Long conversionCount) { this.conversionCount = conversionCount; return this; }
        public Builder conversionRate(Double conversionRate) { this.conversionRate = conversionRate; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public LinkVariantResponse build() { return new LinkVariantResponse(id, shortLinkId, name, destinationUrl, weight, clickCount, conversionCount, conversionRate, isActive, createdAt); }
    }
}
