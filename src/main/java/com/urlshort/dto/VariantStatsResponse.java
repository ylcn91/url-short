package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for A/B test statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A/B test statistics")
public class VariantStatsResponse {

    @Schema(description = "List of all variants with their stats")
    private List<LinkVariantResponse> variants;

    @Schema(description = "Total clicks across all variants", example = "500")
    @JsonProperty("total_clicks")
    private Long totalClicks;

    @Schema(description = "Total conversions across all variants", example = "45")
    @JsonProperty("total_conversions")
    private Long totalConversions;

    @Schema(description = "Overall conversion rate percentage", example = "9.0")
    @JsonProperty("overall_conversion_rate")
    private Double overallConversionRate;
}
