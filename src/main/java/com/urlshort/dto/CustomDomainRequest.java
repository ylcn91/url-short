package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating a custom domain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for custom domain registration")
public class CustomDomainRequest {

    @NotBlank(message = "Domain is required")
    @Pattern(regexp = "^(?!:\\/\\/)([a-zA-Z0-9-_]+\\.)*[a-zA-Z0-9][a-zA-Z0-9-_]+\\.[a-zA-Z]{2,11}?$",
             message = "Must be a valid domain name")
    @Schema(description = "Custom domain name", example = "go.acme.com")
    private String domain;

    @Schema(description = "Whether to use HTTPS", example = "true")
    @JsonProperty("use_https")
    @Builder.Default
    private Boolean useHttps = true;
}
