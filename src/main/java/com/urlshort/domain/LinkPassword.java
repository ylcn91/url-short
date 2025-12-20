package com.urlshort.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * Password protection for short links.
 * Allows links to be protected with a password.
 */
@Entity
@Table(name = "link_passwords", indexes = {
    @Index(name = "idx_link_password", columnList = "short_link_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The short link this password protects.
     * One-to-one relationship.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_link_id", nullable = false, unique = true)
    private ShortLink shortLink;

    /**
     * BCrypt hashed password.
     */
    @NotBlank(message = "Password hash cannot be empty")
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    /**
     * Number of failed password attempts.
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    /**
     * When the link was locked due to too many failed attempts.
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /**
     * When the password was created.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Check if the link is currently locked.
     */
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /**
     * Record a failed password attempt.
     */
    public void recordFailedAttempt() {
        failedAttempts++;
        if (failedAttempts >= 5) {
            // Lock for 15 minutes after 5 failed attempts
            lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    /**
     * Reset failed attempts after successful password entry.
     */
    public void resetFailedAttempts() {
        failedAttempts = 0;
        lockedUntil = null;
    }
}
