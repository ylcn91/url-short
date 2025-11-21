package com.urlshort.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for domain verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Domain verification result")
public class DomainVerificationResponse {

    @Schema(description = "Whether verification succeeded", example = "true")
    private Boolean verified;

    @Schema(description = "Domain name", example = "go.acme.com")
    private String domain;

    @Schema(description = "Current status", example = "VERIFIED")
    private String status;

    @Schema(description = "Verification message")
    private String message;
}
