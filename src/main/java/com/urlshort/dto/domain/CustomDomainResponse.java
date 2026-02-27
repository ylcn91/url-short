package com.urlshort.dto.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for custom domain.
 */
@Schema(description = "Custom domain response")
public record CustomDomainResponse(
        @Schema(description = "Domain ID", example = "1")
        Long id,

        @Schema(description = "Domain name", example = "go.acme.com")
        String domain,

        @Schema(description = "Verification status", example = "VERIFIED")
        String status,

        @Schema(description = "Verification token for DNS TXT record")
        @JsonProperty("verification_token")
        String verificationToken,

        @Schema(description = "When domain was verified")
        @JsonProperty("verified_at")
        LocalDateTime verifiedAt,

        @Schema(description = "Whether to use HTTPS")
        @JsonProperty("use_https")
        Boolean useHttps,

        @Schema(description = "Whether this is the default domain")
        @JsonProperty("is_default")
        Boolean isDefault,

        @Schema(description = "When domain was created")
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String domain;
        private String status;
        private String verificationToken;
        private LocalDateTime verifiedAt;
        private Boolean useHttps;
        private Boolean isDefault;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder verificationToken(String verificationToken) { this.verificationToken = verificationToken; return this; }
        public Builder verifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; return this; }
        public Builder useHttps(Boolean useHttps) { this.useHttps = useHttps; return this; }
        public Builder isDefault(Boolean isDefault) { this.isDefault = isDefault; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CustomDomainResponse build() { return new CustomDomainResponse(id, domain, status, verificationToken, verifiedAt, useHttps, isDefault, createdAt); }
    }
}
