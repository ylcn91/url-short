package com.urlshort.service;

import com.urlshort.dto.LinkHealthResponse;
import com.urlshort.dto.HealthCheckResult;

import java.util.List;

/**
 * Service interface for link health monitoring.
 * <p>
 * Provides operations for checking link health, tracking uptime,
 * and monitoring destination URL availability.
 * </p>
 */
public interface LinkHealthService {

    /**
     * Performs a health check on a short link's destination URL.
     * <p>
     * Makes an HTTP request to the destination URL and records:
     * - HTTP status code
     * - Response time
     * - Success/failure status
     * </p>
     *
     * @param shortLinkId the short link ID
     * @return health check result
     */
    HealthCheckResult performHealthCheck(Long shortLinkId);

    /**
     * Retrieves health status for a short link.
     *
     * @param shortLinkId the short link ID
     * @return current health status and metrics
     */
    LinkHealthResponse getHealth(Long shortLinkId);

    /**
     * Retrieves all unhealthy links for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of unhealthy links
     */
    List<LinkHealthResponse> getUnhealthyLinks(Long workspaceId);

    /**
     * Performs health checks on all links that need checking.
     * <p>
     * Used by background jobs to periodically check link health.
     * Checks links that haven't been checked recently.
     * </p>
     *
     * @return number of links checked
     */
    int performScheduledHealthChecks();

    /**
     * Updates health status based on check result.
     *
     * @param shortLinkId the short link ID
     * @param result the health check result
     */
    void updateHealthStatus(Long shortLinkId, HealthCheckResult result);

    /**
     * Resets health status for a short link.
     *
     * @param shortLinkId the short link ID
     */
    void resetHealth(Long shortLinkId);
}
