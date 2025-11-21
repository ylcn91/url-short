package com.urlshort.service;

import com.urlshort.dto.WebhookRequest;
import com.urlshort.dto.WebhookResponse;
import com.urlshort.dto.WebhookDeliveryResponse;

import java.util.List;
import java.util.Map;

/**
 * Service interface for webhook management and delivery.
 * <p>
 * Provides operations for creating, managing, and triggering webhooks.
 * Handles event subscriptions and HMAC-SHA256 signature generation.
 * </p>
 */
public interface WebhookService {

    /**
     * Creates a new webhook configuration.
     * <p>
     * Generates a random secret for signature verification.
     * </p>
     *
     * @param workspaceId the workspace ID
     * @param request the webhook request
     * @return the created webhook with secret
     */
    WebhookResponse createWebhook(Long workspaceId, WebhookRequest request);

    /**
     * Updates a webhook configuration.
     *
     * @param webhookId the webhook ID
     * @param request the update request
     * @return the updated webhook
     */
    WebhookResponse updateWebhook(Long webhookId, WebhookRequest request);

    /**
     * Triggers webhooks for a specific event.
     * <p>
     * Finds all active webhooks subscribed to the event type and delivers
     * the payload asynchronously.
     * </p>
     *
     * @param workspaceId the workspace ID
     * @param eventType the event type (e.g., "link.created")
     * @param payload the event payload
     */
    void triggerWebhooks(Long workspaceId, String eventType, Map<String, Object> payload);

    /**
     * Delivers a webhook payload to a URL.
     * <p>
     * Signs the payload with HMAC-SHA256 using the webhook secret.
     * Records delivery success/failure.
     * </p>
     *
     * @param webhookId the webhook ID
     * @param payload the event payload
     * @return delivery result
     */
    WebhookDeliveryResponse deliverWebhook(Long webhookId, Map<String, Object> payload);

    /**
     * Retrieves all webhooks for a workspace.
     *
     * @param workspaceId the workspace ID
     * @return list of webhooks
     */
    List<WebhookResponse> getWorkspaceWebhooks(Long workspaceId);

    /**
     * Retrieves a webhook by ID.
     *
     * @param webhookId the webhook ID
     * @return the webhook
     */
    WebhookResponse getWebhook(Long webhookId);

    /**
     * Deletes a webhook.
     *
     * @param webhookId the webhook ID
     */
    void deleteWebhook(Long webhookId);

    /**
     * Regenerates the secret for a webhook.
     *
     * @param webhookId the webhook ID
     * @return the updated webhook with new secret
     */
    WebhookResponse regenerateSecret(Long webhookId);
}
