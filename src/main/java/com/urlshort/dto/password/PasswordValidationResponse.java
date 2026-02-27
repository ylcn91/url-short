package com.urlshort.dto.password;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for password validation.
 */
@Schema(description = "Password validation result")
public record PasswordValidationResponse(
        @Schema(description = "Whether password is valid", example = "true")
        Boolean valid,

        @Schema(description = "Whether link is locked", example = "false")
        Boolean locked,

        @Schema(description = "When lock expires")
        @JsonProperty("locked_until")
        LocalDateTime lockedUntil,

        @Schema(description = "Number of failed attempts")
        @JsonProperty("failed_attempts")
        Integer failedAttempts,

        @Schema(description = "Access token if validation succeeded")
        @JsonProperty("access_token")
        String accessToken,

        @Schema(description = "Validation message")
        String message
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Boolean valid;
        private Boolean locked;
        private LocalDateTime lockedUntil;
        private Integer failedAttempts;
        private String accessToken;
        private String message;

        public Builder valid(Boolean valid) { this.valid = valid; return this; }
        public Builder locked(Boolean locked) { this.locked = locked; return this; }
        public Builder lockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; return this; }
        public Builder failedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public PasswordValidationResponse build() { return new PasswordValidationResponse(valid, locked, lockedUntil, failedAttempts, accessToken, message); }
    }
}
