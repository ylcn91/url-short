package com.urlshort.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshort.domain.Webhook;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.WebhookDeliveryResponse;
import com.urlshort.dto.WebhookRequest;
import com.urlshort.dto.WebhookResponse;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.WebhookRepository;
import com.urlshort.repository.WorkspaceRepository;
import com.urlshort.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of webhook service.
 */
@Service
public class WebhookServiceImpl implements WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private WebhookRepository webhookRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Override
    @Transactional
    public WebhookResponse createWebhook(Long workspaceId, WebhookRequest request) {
        log.info("Creating webhook {} for workspace {}", request.getName(), workspaceId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        String secret = generateSecret();

        Webhook webhook = Webhook.builder()
                .workspace(workspace)
                .name(request.getName())
                .url(request.getUrl())
                .secret(secret)
                .events(new HashSet<>(request.getEvents()))
                .isActive(true)
                .deliveryCount(0L)
                .failureCount(0L)
                .build();

        Webhook saved = webhookRepository.save(webhook);
        log.info("Webhook {} created for workspace {}", request.getName(), workspaceId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public WebhookResponse updateWebhook(Long webhookId, WebhookRequest request) {
        log.info("Updating webhook {}", webhookId);

        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook not found"));

        if (request.getName() != null) {
            webhook.setName(request.getName());
        }
        if (request.getUrl() != null) {
            webhook.setUrl(request.getUrl());
        }
        if (request.getEvents() != null) {
            webhook.setEvents(new HashSet<>(request.getEvents()));
        }
        if (request.getIsActive() != null) {
            webhook.setIsActive(request.getIsActive());
        }

        Webhook saved = webhookRepository.save(webhook);
        return toResponse(saved);
    }

    @Override
    @Async
    @Transactional
    public void triggerWebhooks(Long workspaceId, String eventType, Map<String, Object> payload) {
        log.info("Triggering webhooks for event {} in workspace {}", eventType, workspaceId);

        List<Webhook> webhooks = webhookRepository.findActiveWebhooksForEvent(workspaceId, eventType);

        for (Webhook webhook : webhooks) {
            try {
                deliverWebhook(webhook.getId(), payload);
            } catch (Exception e) {
                log.error("Failed to deliver webhook {}: {}", webhook.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public WebhookDeliveryResponse deliverWebhook(Long webhookId, Map<String, Object> payload) {
        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook not found"));

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            String signature = generateSignature(jsonPayload, webhook.getSecret());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook.getUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                webhook.recordSuccess();
                webhookRepository.save(webhook);
                log.info("Webhook {} delivered successfully", webhookId);

                return WebhookDeliveryResponse.builder()
                        .success(true)
                        .statusCode(response.statusCode())
                        .message("Webhook delivered successfully")
                        .build();
            } else {
                webhook.recordFailure();
                webhookRepository.save(webhook);
                log.warn("Webhook {} delivery failed with status {}", webhookId, response.statusCode());

                return WebhookDeliveryResponse.builder()
                        .success(false)
                        .statusCode(response.statusCode())
                        .message("Webhook delivery failed: HTTP " + response.statusCode())
                        .build();
            }
        } catch (Exception e) {
            webhook.recordFailure();
            webhookRepository.save(webhook);
            log.error("Webhook {} delivery failed: {}", webhookId, e.getMessage());

            return WebhookDeliveryResponse.builder()
                    .success(false)
                    .message("Webhook delivery failed: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookResponse> getWorkspaceWebhooks(Long workspaceId) {
        return webhookRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WebhookResponse getWebhook(Long webhookId) {
        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook not found"));
        return toResponse(webhook);
    }

    @Override
    @Transactional
    public void deleteWebhook(Long webhookId) {
        log.info("Deleting webhook {}", webhookId);
        webhookRepository.deleteById(webhookId);
    }

    @Override
    @Transactional
    public WebhookResponse regenerateSecret(Long webhookId) {
        log.info("Regenerating secret for webhook {}", webhookId);

        Webhook webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook not found"));

        webhook.setSecret(generateSecret());
        Webhook saved = webhookRepository.save(webhook);

        return toResponse(saved);
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());
            return "sha256=" + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    private WebhookResponse toResponse(Webhook webhook) {
        return WebhookResponse.builder()
                .id(webhook.getId())
                .workspaceId(webhook.getWorkspace().getId())
                .name(webhook.getName())
                .url(webhook.getUrl())
                .secret(webhook.getSecret())
                .events(webhook.getEvents())
                .isActive(webhook.getIsActive())
                .deliveryCount(webhook.getDeliveryCount())
                .failureCount(webhook.getFailureCount())
                .successRate(webhook.getSuccessRate())
                .lastStatus(webhook.getLastStatus())
                .lastDeliveryAt(webhook.getLastDeliveryAt())
                .createdAt(webhook.getCreatedAt())
                .build();
    }
}
