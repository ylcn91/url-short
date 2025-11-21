package com.urlshort.repository;

import com.urlshort.domain.LinkPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for LinkPassword entity.
 * Provides database operations for password-protected links.
 */
@Repository
public interface LinkPasswordRepository extends JpaRepository<LinkPassword, Long> {

    /**
     * Finds the password protection for a specific short link.
     * Used when validating password on link access.
     *
     * @param shortLinkId the short link ID
     * @return Optional containing the link password if found
     */
    Optional<LinkPassword> findByShortLinkId(Long shortLinkId);

    /**
     * Checks if a short link has password protection enabled.
     * Used to determine if password validation is required.
     *
     * @param shortLinkId the short link ID
     * @return true if password protection exists
     */
    boolean existsByShortLinkId(Long shortLinkId);

    /**
     * Deletes password protection for a short link.
     * Used when removing password protection.
     *
     * @param shortLinkId the short link ID
     */
    void deleteByShortLinkId(Long shortLinkId);
}
