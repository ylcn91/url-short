package com.urlshort.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Short link entity representing shortened URLs with deterministic reuse support.
 * Core entity for URL shortening. Supports deterministic behavior where the same
 * URL in the same workspace always returns the same short code. The normalized_url
 * field is used for deterministic matching.
 */
@Entity
@Table(
    name = "short_link",
    indexes = {
        @Index(name = "idx_short_link_workspace_code", columnList = "workspace_id, short_code", unique = true),
        @Index(name = "idx_short_link_workspace_normalized_url", columnList = "workspace_id, normalized_url", unique = true),
        @Index(name = "idx_short_link_workspace_created_at", columnList = "workspace_id, created_at"),
        @Index(name = "idx_short_link_created_by", columnList = "created_by"),
        @Index(name = "idx_short_link_expires_at", columnList = "expires_at"),
        @Index(name = "idx_short_link_active", columnList = "workspace_id, created_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Workspace is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_short_link_workspace"))
    private Workspace workspace;

    @NotBlank(message = "Short code is required")
    @Size(max = 20, message = "Short code must not exceed 20 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_-]+$",
        message = "Short code must be alphanumeric with underscores and hyphens"
    )
    @Column(name = "short_code", nullable = false, length = 20)
    private String shortCode;

    @NotBlank(message = "Original URL is required")
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @NotBlank(message = "Normalized URL is required")
    @Column(name = "normalized_url", nullable = false, columnDefinition = "TEXT")
    private String normalizedUrl;

    @Size(max = 500, message = "Title must not exceed 500 characters")
    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Creator is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_short_link_user"))
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Min(value = 0, message = "Click count cannot be negative")
    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Min(value = 1, message = "Max clicks must be at least 1")
    @Column(name = "max_clicks")
    private Long maxClicks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata = new HashMap<>();

    // Relationships

    @JsonIgnore
    @OneToMany(mappedBy = "shortLink", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClickEvent> clickEvents = new ArrayList<>();

    // Business logic methods

    /**
     * Increment the click count.
     * Note: In production, this is typically handled by a database trigger for performance.
     */
    public void incrementClickCount() {
        this.clickCount++;
    }

    /**
     * Check if the link has expired.
     *
     * @return true if the link has expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if the link is currently usable (active, not expired, and not exceeded max clicks).
     *
     * @return true if the link can be used
     */
    public boolean isUsable() {
        return isActive && !isDeleted && !isExpired() && !hasReachedMaxClicks();
    }

    /**
     * Check if the link has reached its maximum allowed clicks.
     *
     * @return true if max clicks limit is set and has been reached
     */
    public boolean hasReachedMaxClicks() {
        return maxClicks != null && clickCount >= maxClicks;
    }

    /**
     * Deactivate this link.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Activate this link.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * Soft delete this link.
     */
    public void softDelete() {
        this.isDeleted = true;
        this.isActive = false;
    }

    /**
     * Restore a soft-deleted link.
     */
    public void restore() {
        this.isDeleted = false;
    }

    /**
     * Get the full short URL for this link.
     *
     * @param baseUrl the base URL (e.g., "https://short.link")
     * @return the full short URL
     */
    public String getFullShortUrl(String baseUrl) {
        return baseUrl + "/" + shortCode;
    }

    // equals and hashCode based on business key (workspace + short_code)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortLink shortLink)) return false;
        return workspace != null && shortCode != null &&
                workspace.equals(shortLink.workspace) &&
                shortCode.equals(shortLink.shortCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, shortCode);
    }

    @Override
    public String toString() {
        return "ShortLink{" +
                "id=" + id +
                ", workspaceId=" + (workspace != null ? workspace.getId() : null) +
                ", shortCode='" + shortCode + '\'' +
                ", originalUrl='" + originalUrl + '\'' +
                ", clickCount=" + clickCount +
                ", isActive=" + isActive +
                ", isDeleted=" + isDeleted +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
