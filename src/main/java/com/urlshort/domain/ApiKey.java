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
 *
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> scopes = new ArrayList<>();

    // Business logic methods

    /**
     * Update the last used timestamp to now.
     */
    public void updateLastUsed() {
        this.lastUsedAt = Instant.now();
    }

    /**
     * Check if the API key has expired.
     *
     * @return true if the key has expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the API key is currently usable (active and not expired).
     *
     * @return true if the key can be used
     */
    public boolean isUsable() {
        return isActive && !isExpired();
    }

    /**
     * Deactivate this API key.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Activate this API key.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Check if this API key has a specific scope.
     *
     * @param scope the scope to check (e.g., "links:read", "links:write")
     * @return true if the key has the specified scope
     */
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }

    /**
     * Check if this API key has all specified scopes.
     *
     * @param requiredScopes the scopes to check
     * @return true if the key has all specified scopes
     */
    public boolean hasAllScopes(List<String> requiredScopes) {
        return scopes != null && scopes.containsAll(requiredScopes);
    }

    /**
     * Check if this API key has any of the specified scopes.
     *
     * @param requiredScopes the scopes to check
     * @return true if the key has at least one of the specified scopes
     */
    public boolean hasAnyScope(List<String> requiredScopes) {
        if (scopes == null || requiredScopes == null) {
            return false;
        }
        return requiredScopes.stream().anyMatch(scopes::contains);
    }

    /**
     * Add a scope to this API key.
     *
     * @param scope the scope to add
     */
    public void addScope(String scope) {
        if (scopes == null) {
            scopes = new ArrayList<>();
        }
        if (!scopes.contains(scope)) {
            scopes.add(scope);
        }
    }

    /**
     * Remove a scope from this API key.
     *
     * @param scope the scope to remove
     */
    public void removeScope(String scope) {
        if (scopes != null) {
            scopes.remove(scope);
        }
    }

    /**
     * Get a masked version of the key prefix for display (e.g., "sk_live_abc...").
     *
     * @return masked key prefix
     */
    public String getMaskedKey() {
        if (keyPrefix == null || keyPrefix.length() < 8) {
            return keyPrefix;
        }
        return keyPrefix.substring(0, Math.min(12, keyPrefix.length())) + "...";
    }

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
