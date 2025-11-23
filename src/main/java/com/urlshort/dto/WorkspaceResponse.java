package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO containing workspace details.
 * Implemented as an immutable Java record.
 */
@Schema(description = "Response object containing workspace details")
public record WorkspaceResponse(

        @Schema(description = "Unique identifier of the workspace", example = "1")
        Long id,

        @Schema(description = "Workspace name", example = "Acme Corporation")
        String name,

        @Schema(description = "URL-safe workspace slug", example = "acme-corp")
        String slug,

        @Schema(description = "Timestamp when the workspace was created", example = "2025-11-18T10:30:00Z")
        @JsonProperty("created_at")
        Instant createdAt,

        @Schema(description = "Timestamp when the workspace was last updated", example = "2025-11-18T10:30:00Z")
        @JsonProperty("updated_at")
        Instant updatedAt,

        @Schema(description = "Whether the workspace is active", example = "true")
        @JsonProperty("is_active")
        Boolean isActive,

        @Schema(description = "Workspace settings and configuration")
        Map<String, Object> settings
) {
    /**
     * Builder pattern for WorkspaceResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String slug;
        private Instant createdAt;
        private Instant updatedAt;
        private Boolean isActive;
        private Map<String, Object> settings;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder settings(Map<String, Object> settings) {
            this.settings = settings;
            return this;
        }

        public WorkspaceResponse build() {
            return new WorkspaceResponse(id, name, slug, createdAt, updatedAt, isActive, settings);
        }
    }
}
