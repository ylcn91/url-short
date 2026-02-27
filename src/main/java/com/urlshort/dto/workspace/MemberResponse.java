package com.urlshort.dto.workspace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.urlshort.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Response DTO containing workspace member details.
 * Implemented as an immutable Java record.
 */
@Schema(description = "Response object containing workspace member details")
public record MemberResponse(

        @Schema(description = "Unique identifier of the user", example = "123")
        Long id,

        @Schema(description = "Member's email address", example = "user@example.com")
        String email,

        @Schema(description = "Member's full name", example = "John Doe")
        @JsonProperty("full_name")
        String fullName,

        @Schema(description = "Member's role in the workspace", example = "MEMBER")
        UserRole role,

        @Schema(description = "Timestamp when the member was created", example = "2025-11-18T10:30:00Z")
        @JsonProperty("created_at")
        Instant createdAt,

        @Schema(description = "Timestamp of member's last login", example = "2025-11-18T15:45:00Z")
        @JsonProperty("last_login_at")
        Instant lastLoginAt,

        @Schema(description = "Whether the member is active", example = "true")
        @JsonProperty("is_active")
        Boolean isActive
) {
    /**
     * Builder pattern for MemberResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String email;
        private String fullName;
        private UserRole role;
        private Instant createdAt;
        private Instant lastLoginAt;
        private Boolean isActive;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public Builder role(UserRole role) {
            this.role = role;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastLoginAt(Instant lastLoginAt) {
            this.lastLoginAt = lastLoginAt;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public MemberResponse build() {
            return new MemberResponse(id, email, fullName, role, createdAt, lastLoginAt, isActive);
        }
    }
}
