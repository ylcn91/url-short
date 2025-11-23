package com.urlshort.repository;

import com.urlshort.domain.ShortLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ShortLink entity.
 * Provides database operations for URL shortening and link management.
 */
@Repository
public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    /**
     * Critical lookup method for handling redirects.
     * Finds a short link by workspace and short code, excluding soft-deleted records.
     * This is the primary method used when users access shortened URLs.
     *
     * @param workspaceId the workspace ID
     * @param shortCode the unique short code
     * @return Optional containing the short link if found
     */
    Optional<ShortLink> findByWorkspaceIdAndShortCodeAndIsDeletedFalse(Long workspaceId, String shortCode);

    /**
     * Deterministic lookup to check if a URL already exists in the workspace.
     * Uses normalized URL to prevent duplicate shortened links for the same destination.
     *
     * @param workspaceId the workspace ID
     * @param normalizedUrl the normalized destination URL
     * @return Optional containing the existing short link if found
     */
    Optional<ShortLink> findByWorkspaceIdAndNormalizedUrlAndIsDeletedFalse(Long workspaceId, String normalizedUrl);

    /**
     * Retrieves all active short links for a workspace with pagination support.
     * Used for listing and managing links in the workspace dashboard.
     *
     * @param workspaceId the workspace ID
     * @param pageable pagination parameters
     * @return page of active short links
     */
    Page<ShortLink> findByWorkspaceIdAndIsDeletedFalse(Long workspaceId, Pageable pageable);

    /**
     * Counts the total number of active short links in a workspace.
     * Used for analytics and quota management.
     *
     * @param workspaceId the workspace ID
     * @return count of active short links
     */
    long countByWorkspaceIdAndIsDeletedFalse(Long workspaceId);

    /**
     * Finds all expired links that are still marked as active.
     * Used by background jobs to automatically deactivate expired links.
     *
     * Query checks for links where:
     * - expiresAt is in the past (before the provided timestamp)
     * - isActive is still true
     *
     * @param now the current timestamp
     * @return list of expired but still active links
     */
    @Query("SELECT sl FROM ShortLink sl WHERE sl.expiresAt < :now AND sl.isActive = true")
    List<ShortLink> findExpiredLinks(@Param("now") LocalDateTime now);

    /**
     * Finds all links that have exceeded their maximum click limit.
     * Used by background jobs to automatically deactivate links that reached their limit.
     *
     * Query checks for links where:
     * - maxClicks is set (not null)
     * - clickCount has reached or exceeded maxClicks
     * - isActive is still true
     *
     * @return list of links that exceeded their click limit
     */
    @Query("SELECT sl FROM ShortLink sl WHERE sl.maxClicks IS NOT NULL AND sl.clickCount >= sl.maxClicks AND sl.isActive = true")
    List<ShortLink> findLinksExceedingMaxClicks();
}
