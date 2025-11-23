package com.urlshort.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
 * Multi-tenant workspace entity representing a tenant/organization.
 *
 * Workspaces provide tenant isolation for the URL shortener platform. Each workspace
 * has its own set of users, short links, and API keys. The slug is used for subdomain
 * routing and URL-safe identification.
 */
@Entity
@Table(
    name = "workspace",
    indexes = {
        @Index(name = "idx_workspace_slug", columnList = "slug", unique = true),
        @Index(name = "idx_workspace_is_deleted", columnList = "is_deleted")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Workspace name is required")
    @Size(max = 255, message = "Workspace name must not exceed 255 characters")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Workspace slug is required")
    @Size(max = 100, message = "Workspace slug must not exceed 100 characters")
    @Pattern(
        regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
        message = "Slug must be lowercase alphanumeric with hyphens, starting and ending with alphanumeric"
    )
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> settings = new HashMap<>();

    // Relationships

    @JsonIgnore
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShortLink> shortLinks = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ApiKey> apiKeys = new ArrayList<>();

    // Business logic methods

    /**
     * Soft delete this workspace.
     */
    public void softDelete() {
        this.isDeleted = true;
    }

    /**
     * Restore a soft-deleted workspace.
     */
    public void restore() {
        this.isDeleted = false;
    }

    /**
     * Check if the workspace is active (not deleted).
     *
     * @return true if active, false if soft-deleted
     */
    public boolean isActive() {
        return !isDeleted;
    }

    // equals and hashCode based on business key (slug)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Workspace workspace)) return false;
        return slug != null && slug.equals(workspace.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slug);
    }

    @Override
    public String toString() {
        return "Workspace{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", isDeleted=" + isDeleted +
                ", createdAt=" + createdAt +
                '}';
    }
}
