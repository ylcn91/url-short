package com.urlshort.controller;

import com.urlshort.dto.common.ApiResponse;
import com.urlshort.dto.webhook.WebhookRequest;
import com.urlshort.dto.webhook.WebhookResponse;
import com.urlshort.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for webhook management.
 * Base path: /api/v1/webhooks
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Endpoints for webhook configuration and event notifications")
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Create webhook",
               description = "Create a new webhook configuration. Returns webhook with generated secret.")
    public ResponseEntity<ApiResponse<WebhookResponse>> createWebhook(
            @Parameter(description = "Workspace ID") @RequestParam Long workspaceId,
            @Valid @RequestBody WebhookRequest request) {
        log.info("Creating webhook {} for workspace {}", request.getName(), workspaceId);
        WebhookResponse response = webhookService.createWebhook(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{webhookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Update webhook",
               description = "Update webhook configuration")
    public ResponseEntity<ApiResponse<WebhookResponse>> updateWebhook(
            @Parameter(description = "Webhook ID") @PathVariable Long webhookId,
            @Valid @RequestBody WebhookRequest request) {
        log.info("Updating webhook {}", webhookId);
        WebhookResponse response = webhookService.updateWebhook(webhookId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Get all webhooks",
               description = "Retrieve all webhooks for a workspace")
    public ResponseEntity<ApiResponse<List<WebhookResponse>>> getWorkspaceWebhooks(
            @Parameter(description = "Workspace ID") @RequestParam Long workspaceId) {
        log.info("Getting webhooks for workspace {}", workspaceId);
        List<WebhookResponse> response = webhookService.getWorkspaceWebhooks(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{webhookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Get webhook by ID",
               description = "Retrieve webhook configuration by ID")
    public ResponseEntity<ApiResponse<WebhookResponse>> getWebhook(
            @Parameter(description = "Webhook ID") @PathVariable Long webhookId) {
        log.info("Getting webhook {}", webhookId);
        WebhookResponse response = webhookService.getWebhook(webhookId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{webhookId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Delete webhook",
               description = "Delete a webhook configuration")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(
            @Parameter(description = "Webhook ID") @PathVariable Long webhookId) {
        log.info("Deleting webhook {}", webhookId);
        webhookService.deleteWebhook(webhookId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{webhookId}/regenerate-secret")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerate webhook secret",
               description = "Regenerate the secret for webhook signature verification (Admin only)")
    public ResponseEntity<ApiResponse<WebhookResponse>> regenerateSecret(
            @Parameter(description = "Webhook ID") @PathVariable Long webhookId) {
        log.info("Regenerating secret for webhook {}", webhookId);
        WebhookResponse response = webhookService.regenerateSecret(webhookId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
