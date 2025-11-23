package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating an A/B test variant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for A/B test variant")
public class LinkVariantRequest {

    @NotBlank(message = "Variant name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    @Schema(description = "Variant name", example = "Control")
    private String name;

    @NotBlank(message = "Destination URL is required")
    @org.hibernate.validator.constraints.URL(message = "Must be a valid URL")
    @Schema(description = "Destination URL for this variant", example = "https://example.com/page-a")
    @JsonProperty("destination_url")
    private String destinationUrl;

    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight must be at least 0")
    @Max(value = 100, message = "Weight must not exceed 100")
    @Schema(description = "Traffic allocation percentage (0-100)", example = "50")
    private Integer weight;
}
