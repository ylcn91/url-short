package com.urlshort.dto.password;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for link password protection.
 */
@Schema(description = "Password protection response")
public record LinkPasswordResponse(
        @Schema(description = "Password protection ID", example = "1")
        Long id,

        @Schema(description = "Short link ID", example = "100")
        @JsonProperty("short_link_id")
        Long shortLinkId,

        @Schema(description = "Number of failed attempts", example = "0")
        @JsonProperty("failed_attempts")
        Integer failedAttempts,

        @Schema(description = "Whether link is currently locked", example = "false")
        Boolean locked,

        @Schema(description = "When lock expires")
        @JsonProperty("locked_until")
        LocalDateTime lockedUntil,

        @Schema(description = "When password was created")
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long shortLinkId;
        private Integer failedAttempts;
        private Boolean locked;
        private LocalDateTime lockedUntil;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder shortLinkId(Long shortLinkId) { this.shortLinkId = shortLinkId; return this; }
        public Builder failedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; return this; }
        public Builder locked(Boolean locked) { this.locked = locked; return this; }
        public Builder lockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public LinkPasswordResponse build() { return new LinkPasswordResponse(id, shortLinkId, failedAttempts, locked, lockedUntil, createdAt); }
    }
}
