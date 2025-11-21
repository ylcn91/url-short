package com.urlshort.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User entity representing user accounts within workspaces.
 *
 * Users belong to exactly one workspace and have a role that determines their
 * access level. Email addresses must be unique within a workspace but can be
 * reused across different workspaces.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_workspace_email", columnList = "workspace_id, email", unique = true),
        @Index(name = "idx_users_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_users_is_deleted", columnList = "is_deleted")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Workspace is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_workspace"))
    private Workspace workspace;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Column(nullable = false)
    private String email;

    @JsonIgnore
    @NotBlank(message = "Password hash is required")
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotNull(message = "User role is required")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.MEMBER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // Relationships

    @JsonIgnore
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.PERSIST)
    @Builder.Default
    private List<ShortLink> shortLinks = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.PERSIST)
    @Builder.Default
    private List<ApiKey> apiKeys = new ArrayList<>();

    // Business logic methods

    /**
     * Update the last login timestamp to now.
     */
    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
    }

    /**
     * Soft delete this user.
     */
    public void softDelete() {
        this.isDeleted = true;
    }

    /**
     * Restore a soft-deleted user.
     */
    public void restore() {
        this.isDeleted = false;
    }

    /**
     * Check if the user is active (not deleted).
     *
     * @return true if active, false if soft-deleted
     */
    public boolean isActive() {
        return !isDeleted;
    }

    /**
     * Check if the user has admin role.
     *
     * @return true if user is an admin
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Check if the user has member or higher role.
     *
     * @return true if user is a member or admin
     */
    public boolean isMemberOrHigher() {
        return role == UserRole.MEMBER || role == UserRole.ADMIN;
    }

    /**
     * Check if the user can only view (viewer role).
     *
     * @return true if user is a viewer
     */
    public boolean isViewer() {
        return role == UserRole.VIEWER;
    }

    // equals and hashCode based on business key (workspace + email)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return workspace != null && email != null &&
                workspace.equals(user.workspace) &&
                email.equals(user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", workspaceId=" + (workspace != null ? workspace.getId() : null) +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", isDeleted=" + isDeleted +
                ", createdAt=" + createdAt +
                '}';
    }
}
