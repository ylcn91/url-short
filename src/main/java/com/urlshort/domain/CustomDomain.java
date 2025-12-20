package com.urlshort.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/**
 * Custom Domain entity for workspace-specific branded domains.
 * Allows workspaces to use their own domains for short links.
 *
 * Examples:
 * - go.acme.com
 * - links.company.io
 * - s.brand.com
 */
@Entity
@Table(name = "custom_domains", indexes = {
    @Index(name = "idx_custom_domain", columnList = "domain", unique = true),
    @Index(name = "idx_workspace_domain", columnList = "workspace_id"),
    @Index(name = "idx_domain_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The workspace that owns this custom domain.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /**
     * The custom domain (e.g., "go.acme.com").
     * Must be unique across all workspaces.
     */
    @NotBlank(message = "Domain cannot be empty")
    @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$",
             message = "Invalid domain format")
    @Column(nullable = false, unique = true, length = 255)
    private String domain;

    /**
     * Verification status of the domain.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DomainStatus status = DomainStatus.PENDING;

    /**
     * Verification token for DNS TXT record verification.
     * Format: linkforge-verify=<token>
     */
    @Column(name = "verification_token", nullable = false, length = 64)
    private String verificationToken;

    /**
     * When the domain was successfully verified.
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * Whether this domain should use HTTPS (SSL).
     * Must be true for custom domains.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean useHttps = true;

    /**
     * Whether this is the default domain for the workspace.
     * Only one domain per workspace can be default.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * When the domain was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When the domain was last updated.
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Domain verification status.
     */
    public enum DomainStatus {
        /**
         * Domain is pending verification.
         * User needs to add DNS records.
         */
        PENDING,

        /**
         * Domain is verified and active.
         */
        VERIFIED,

        /**
         * Verification failed.
         */
        FAILED,

        /**
         * Domain is disabled (manual action).
         */
        DISABLED
    }
}
