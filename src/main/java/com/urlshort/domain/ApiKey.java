package com.urlshort.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * API key entity for programmatic access to the platform.
 * API keys are scoped to workspaces and support fine-grained permissions through
 * scopes. Keys are stored as SHA-256 hashes for security. Uses hard delete strategy
 * - expired keys are permanently removed after a grace period.
 */
@Entity
@Table(
    name = "api_key",
    indexes = {
        @Index(name = "idx_api_key_hash", columnList = "key_hash", unique = true),
        @Index(name = "idx_api_key_workspace_prefix", columnList = "workspace_id, key_prefix", unique = true),
        @Index(name = "idx_api_key_workspace_active", columnList = "workspace_id, is_active"),
        @Index(name = "idx_api_key_expires_at", columnList = "expires_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Workspace is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_api_key_workspace"))
    private Workspace workspace;

    @JsonIgnore
    @NotBlank(message = "Key hash is required")
    @Size(max = 64, message = "Key hash must be 64 characters (SHA-256)")
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @NotBlank(message = "Key prefix is required")
    @Size(max = 20, message = "Key prefix must not exceed 20 characters")
    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @NotBlank(message = "API key name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Creator is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_api_key_user"))
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> scopes = new ArrayList<>();

    // equals and hashCode based on business key (key_hash)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiKey apiKey)) return false;
        return keyHash != null && keyHash.equals(apiKey.keyHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyHash);
    }

    @Override
    public String toString() {
        return "ApiKey{" +
                "id=" + id +
                ", workspaceId=" + (workspace != null ? workspace.getId() : null) +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", name='" + name + '\'' +
                ", isActive=" + isActive +
                ", expiresAt=" + expiresAt +
                ", scopes=" + scopes +
                ", createdAt=" + createdAt +
                '}';
    }
}
