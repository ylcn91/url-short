package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for custom domain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Custom domain response")
public class CustomDomainResponse {

    @Schema(description = "Domain ID", example = "1")
    private Long id;

    @Schema(description = "Domain name", example = "go.acme.com")
    private String domain;

    @Schema(description = "Verification status", example = "VERIFIED")
    private String status;

    @Schema(description = "Verification token for DNS TXT record")
    @JsonProperty("verification_token")
    private String verificationToken;

    @Schema(description = "When domain was verified")
    @JsonProperty("verified_at")
    private LocalDateTime verifiedAt;

    @Schema(description = "Whether to use HTTPS")
    @JsonProperty("use_https")
    private Boolean useHttps;

    @Schema(description = "Whether this is the default domain")
    @JsonProperty("is_default")
    private Boolean isDefault;

    @Schema(description = "When domain was created")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
