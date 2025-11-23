package com.urlshort.repository;

import com.urlshort.domain.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Webhook entity.
 * Provides database operations for webhook management and delivery.
 */
@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {

    /**
     * Retrieves all webhooks for a workspace.
     * Used for webhook management dashboard.
     *
     * @param workspaceId the workspace ID
     * @return list of webhooks
     */
    List<Webhook> findByWorkspaceId(Long workspaceId);

    /**
     * Retrieves all active webhooks for a workspace.
     * Used for webhook management and filtering.
     *
     * @param workspaceId the workspace ID
     * @param isActive true to get only active webhooks
     * @return list of active webhooks
     */
    List<Webhook> findByWorkspaceIdAndIsActive(Long workspaceId, Boolean isActive);

    /**
     * Finds all active webhooks subscribed to a specific event type.
     * Used when triggering webhooks for an event.
     *
     * Query checks for webhooks where:
     * - The webhook belongs to the specified workspace
     * - The webhook is active
     * - The webhook's events collection contains the specified event type
     *
     * @param workspaceId the workspace ID
     * @param eventType the event type (e.g., "link.created")
     * @return list of webhooks subscribed to the event
     */
    @Query("SELECT w FROM Webhook w JOIN w.events e WHERE w.workspace.id = :workspaceId AND w.isActive = true AND e = :eventType")
    List<Webhook> findActiveWebhooksForEvent(@Param("workspaceId") Long workspaceId, @Param("eventType") String eventType);

    /**
     * Counts active webhooks for a workspace.
     * Used for quota management.
     *
     * @param workspaceId the workspace ID
     * @param isActive true to count only active webhooks
     * @return count of active webhooks
     */
    long countByWorkspaceIdAndIsActive(Long workspaceId, Boolean isActive);
}
