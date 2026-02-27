package com.urlshort.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for domain verification.
 */
@Schema(description = "Domain verification result")
public record DomainVerificationResponse(
        @Schema(description = "Whether verification succeeded", example = "true")
        Boolean verified,

        @Schema(description = "Domain name", example = "go.acme.com")
        String domain,

        @Schema(description = "Current status", example = "VERIFIED")
        String status,

        @Schema(description = "Verification message")
        String message
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Boolean verified;
        private String domain;
        private String status;
        private String message;

        public Builder verified(Boolean verified) { this.verified = verified; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public DomainVerificationResponse build() { return new DomainVerificationResponse(verified, domain, status, message); }
    }
}
