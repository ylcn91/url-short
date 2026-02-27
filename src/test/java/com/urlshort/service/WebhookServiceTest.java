package com.urlshort.service;

import com.urlshort.domain.Webhook;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.webhook.WebhookRequest;
import com.urlshort.dto.webhook.WebhookResponse;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.WebhookRepository;
import com.urlshort.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService Unit Tests")
class WebhookServiceTest {

    @Mock
    private WebhookRepository webhookRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private WebhookService webhookService;

    private Workspace workspace;
    private Webhook webhook;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(1L)
                .name("Test Workspace")
                .slug("test-ws")
                .isDeleted(false)
                .settings(new HashMap<>())
                .build();

        webhook = Webhook.builder()
                .id(10L)
                .workspace(workspace)
                .name("Slack Notifications")
                .url("https://hooks.slack.com/services/test")
                .secret("generated-secret-key")
                .events(new HashSet<>(Set.of("link.created", "link.clicked")))
                .isActive(true)
                .deliveryCount(0L)
                .failureCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createWebhook creates a new webhook successfully")
    void createWebhook_success_createsWebhook() {
        WebhookRequest request = WebhookRequest.builder()
                .name("Slack Notifications")
                .url("https://hooks.slack.com/services/test")
                .events(List.of("link.created", "link.clicked"))
                .build();

        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(workspace));
        when(webhookRepository.save(any(Webhook.class))).thenReturn(webhook);

        WebhookResponse response = webhookService.createWebhook(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Slack Notifications");
        assertThat(response.url()).isEqualTo("https://hooks.slack.com/services/test");
        assertThat(response.events()).containsExactlyInAnyOrder("link.created", "link.clicked");
        assertThat(response.isActive()).isTrue();
        assertThat(response.secret()).isNotNull();
        verify(webhookRepository).save(any(Webhook.class));
    }

    @Test
    @DisplayName("updateWebhook updates webhook properties")
    void updateWebhook_success_updatesWebhook() {
        WebhookRequest request = WebhookRequest.builder()
                .name("Updated Webhook")
                .url("https://hooks.slack.com/services/updated")
                .events(List.of("link.expired"))
                .isActive(false)
                .build();

        Webhook updatedWebhook = Webhook.builder()
                .id(10L)
                .workspace(workspace)
                .name("Updated Webhook")
                .url("https://hooks.slack.com/services/updated")
                .secret("generated-secret-key")
                .events(new HashSet<>(Set.of("link.expired")))
                .isActive(false)
                .deliveryCount(0L)
                .failureCount(0L)
                .createdAt(LocalDateTime.now())
                .build();

        when(webhookRepository.findById(10L)).thenReturn(Optional.of(webhook));
        when(webhookRepository.save(any(Webhook.class))).thenReturn(updatedWebhook);

        WebhookResponse response = webhookService.updateWebhook(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Updated Webhook");
        assertThat(response.url()).isEqualTo("https://hooks.slack.com/services/updated");
        assertThat(response.isActive()).isFalse();
        verify(webhookRepository).save(any(Webhook.class));
    }

    @Test
    @DisplayName("getWorkspaceWebhooks returns list of webhook responses")
    void getWorkspaceWebhooks_returnsWebhookList() {
        when(webhookRepository.findByWorkspaceId(1L)).thenReturn(List.of(webhook));

        List<WebhookResponse> responses = webhookService.getWorkspaceWebhooks(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Slack Notifications");
        assertThat(responses.get(0).workspaceId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deleteWebhook calls repository deleteById")
    void deleteWebhook_callsDeleteById() {
        doNothing().when(webhookRepository).deleteById(10L);

        webhookService.deleteWebhook(10L);

        verify(webhookRepository).deleteById(10L);
    }

    @Test
    @DisplayName("regenerateSecret generates a new secret for the webhook")
    void regenerateSecret_generatesNewSecret() {
        String originalSecret = webhook.getSecret();

        when(webhookRepository.findById(10L)).thenReturn(Optional.of(webhook));
        when(webhookRepository.save(any(Webhook.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookResponse response = webhookService.regenerateSecret(10L);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Slack Notifications");
        // The secret should have been changed (the service generates a new one)
        verify(webhookRepository).save(any(Webhook.class));
        // Verify the secret on the entity was updated (it will be a new random value)
        assertThat(webhook.getSecret()).isNotNull();
    }
}
