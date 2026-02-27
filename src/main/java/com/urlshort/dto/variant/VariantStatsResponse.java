package com.urlshort.dto.variant;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for A/B test statistics.
 */
@Schema(description = "A/B test statistics")
public record VariantStatsResponse(
        @Schema(description = "List of all variants with their stats")
        List<LinkVariantResponse> variants,

        @Schema(description = "Total clicks across all variants", example = "500")
        @JsonProperty("total_clicks")
        Long totalClicks,

        @Schema(description = "Total conversions across all variants", example = "45")
        @JsonProperty("total_conversions")
        Long totalConversions,

        @Schema(description = "Overall conversion rate percentage", example = "9.0")
        @JsonProperty("overall_conversion_rate")
        Double overallConversionRate
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<LinkVariantResponse> variants;
        private Long totalClicks;
        private Long totalConversions;
        private Double overallConversionRate;

        public Builder variants(List<LinkVariantResponse> variants) { this.variants = variants; return this; }
        public Builder totalClicks(Long totalClicks) { this.totalClicks = totalClicks; return this; }
        public Builder totalConversions(Long totalConversions) { this.totalConversions = totalConversions; return this; }
        public Builder overallConversionRate(Double overallConversionRate) { this.overallConversionRate = overallConversionRate; return this; }
        public VariantStatsResponse build() { return new VariantStatsResponse(variants, totalClicks, totalConversions, overallConversionRate); }
    }
}
