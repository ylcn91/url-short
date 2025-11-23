package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for A/B test variant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A/B test variant response")
public class LinkVariantResponse {

    @Schema(description = "Variant ID", example = "1")
    private Long id;

    @Schema(description = "Short link ID", example = "100")
    @JsonProperty("short_link_id")
    private Long shortLinkId;

    @Schema(description = "Variant name", example = "Control")
    private String name;

    @Schema(description = "Destination URL", example = "https://example.com/page-a")
    @JsonProperty("destination_url")
    private String destinationUrl;

    @Schema(description = "Traffic weight percentage", example = "50")
    private Integer weight;

    @Schema(description = "Total clicks for this variant", example = "245")
    @JsonProperty("click_count")
    private Long clickCount;

    @Schema(description = "Total conversions for this variant", example = "23")
    @JsonProperty("conversion_count")
    private Long conversionCount;

    @Schema(description = "Conversion rate percentage", example = "9.39")
    @JsonProperty("conversion_rate")
    private Double conversionRate;

    @Schema(description = "Whether variant is active", example = "true")
    @JsonProperty("is_active")
    private Boolean isActive;

    @Schema(description = "When variant was created")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
