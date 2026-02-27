package com.urlshort.repository;

import com.urlshort.domain.LinkHealth;
import com.urlshort.domain.LinkHealth.HealthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for LinkHealth entity.
 * Provides database operations for link health monitoring.
 */
@Repository
public interface LinkHealthRepository extends JpaRepository<LinkHealth, Long> {

    /**
     * Finds the health record for a specific short link.
     * Used to display health status and metrics.
     *
     * @param shortLinkId the short link ID
     * @return Optional containing the health record if found
     */
    Optional<LinkHealth> findByShortLinkId(Long shortLinkId);

    /**
     * Finds all links that need health checks.
     * Used by background jobs to schedule health checks.
     *
     * Query finds links where:
     * - Last check was before the specified time (or never checked)
     * - Link is not already marked as DOWN
     *
     * @param beforeTime the time threshold for last check
     * @return list of links needing health checks
     */
    @Query("SELECT lh FROM LinkHealth lh WHERE (lh.lastCheckedAt IS NULL OR lh.lastCheckedAt < :beforeTime) AND lh.status != 'DOWN'")
    List<LinkHealth> findLinksNeedingHealthCheck(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Finds all unhealthy links for a workspace.
     * Used for alerts and monitoring dashboard.
     *
     * @param workspaceId the workspace ID
     * @param status the health status
     * @return list of unhealthy links
     */
    @Query("SELECT lh FROM LinkHealth lh WHERE lh.shortLink.workspace.id = :workspaceId AND lh.status = :status")
    List<LinkHealth> findByWorkspaceIdAndStatus(@Param("workspaceId") Long workspaceId, @Param("status") HealthStatus status);

    /**
     * Deletes health record for a short link.
     * Used when a link is deleted.
     *
     * @param shortLinkId the short link ID
     */
    void deleteByShortLinkId(Long shortLinkId);
}
