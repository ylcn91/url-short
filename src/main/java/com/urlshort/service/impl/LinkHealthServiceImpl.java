package com.urlshort.service.impl;

import com.urlshort.domain.LinkHealth;
import com.urlshort.domain.ShortLink;
import com.urlshort.dto.HealthCheckResult;
import com.urlshort.dto.LinkHealthResponse;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.LinkHealthRepository;
import com.urlshort.repository.ShortLinkRepository;
import com.urlshort.service.LinkHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of link health monitoring service.
 */
@Service
public class LinkHealthServiceImpl implements LinkHealthService {

    private static final Logger log = LoggerFactory.getLogger(LinkHealthServiceImpl.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Autowired
    private LinkHealthRepository healthRepository;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Override
    @Transactional
    public HealthCheckResult performHealthCheck(Long shortLinkId) {
        log.info("Performing health check for link {}", shortLinkId);

        ShortLink shortLink = shortLinkRepository.findById(shortLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));

        long startTime = System.currentTimeMillis();
        HealthCheckResult result;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(shortLink.getOriginalUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            long responseTime = System.currentTimeMillis() - startTime;

            int statusCode = response.statusCode();
            boolean healthy = statusCode >= 200 && statusCode < 400;

            result = HealthCheckResult.builder()
                    .shortLinkId(shortLinkId)
                    .healthy(healthy)
                    .statusCode(statusCode)
                    .responseTimeMs(responseTime)
                    .error(null)
                    .checkedAt(LocalDateTime.now())
                    .build();

            log.info("Health check for link {} completed: status={}, time={}ms",
                    shortLinkId, statusCode, responseTime);

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result = HealthCheckResult.builder()
                    .shortLinkId(shortLinkId)
                    .healthy(false)
                    .statusCode(null)
                    .responseTimeMs(responseTime)
                    .error(e.getMessage())
                    .checkedAt(LocalDateTime.now())
                    .build();

            log.error("Health check for link {} failed: {}", shortLinkId, e.getMessage());
        }

        updateHealthStatus(shortLinkId, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public LinkHealthResponse getHealth(Long shortLinkId) {
        LinkHealth health = healthRepository.findByShortLinkId(shortLinkId)
                .orElse(createDefaultHealth(shortLinkId));
        return toResponse(health);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkHealthResponse> getUnhealthyLinks(Long workspaceId) {
        List<LinkHealth> unhealthyLinks = new ArrayList<>(healthRepository.findByWorkspaceIdAndStatus(workspaceId, LinkHealth.HealthStatus.UNHEALTHY));
        unhealthyLinks.addAll(healthRepository.findByWorkspaceIdAndStatus(workspaceId, LinkHealth.HealthStatus.DOWN));
        unhealthyLinks.addAll(healthRepository.findByWorkspaceIdAndStatus(workspaceId, LinkHealth.HealthStatus.DEGRADED));

        return unhealthyLinks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int performScheduledHealthChecks() {
        log.info("Starting scheduled health checks");

        // Check links that haven't been checked in the last hour
        LocalDateTime threshold = LocalDateTime.now().minusHours(1);
        List<LinkHealth> linksToCheck = healthRepository.findLinksNeedingHealthCheck(threshold);

        int checkedCount = 0;
        for (LinkHealth linkHealth : linksToCheck) {
            try {
                performHealthCheck(linkHealth.getShortLink().getId());
                checkedCount++;
            } catch (Exception e) {
                log.error("Failed to check link {}: {}", linkHealth.getShortLink().getId(), e.getMessage());
            }
        }

        log.info("Scheduled health checks completed: {} links checked", checkedCount);
        return checkedCount;
    }

    @Override
    @Transactional
    public void updateHealthStatus(Long shortLinkId, HealthCheckResult result) {
        LinkHealth health = healthRepository.findByShortLinkId(shortLinkId)
                .orElse(createDefaultHealth(shortLinkId));

        health.setLastCheckedAt(result.getCheckedAt());
        health.setLastStatusCode(result.getStatusCode());
        health.setLastResponseTimeMs(result.getResponseTimeMs());
        health.setLastError(result.getError());
        health.setCheckCount(health.getCheckCount() + 1);

        if (result.isHealthy()) {
            health.setSuccessCount(health.getSuccessCount() + 1);
            health.setConsecutiveFailures(0);

            // Determine status based on response time
            if (result.getResponseTimeMs() < 1000) {
                health.setStatus(LinkHealth.HealthStatus.HEALTHY);
            } else if (result.getResponseTimeMs() < 3000) {
                health.setStatus(LinkHealth.HealthStatus.DEGRADED);
            } else {
                health.setStatus(LinkHealth.HealthStatus.UNHEALTHY);
            }
        } else {
            health.setConsecutiveFailures(health.getConsecutiveFailures() + 1);

            // Determine status based on consecutive failures
            if (health.getConsecutiveFailures() >= 5) {
                health.setStatus(LinkHealth.HealthStatus.DOWN);
            } else if (health.getConsecutiveFailures() >= 3) {
                health.setStatus(LinkHealth.HealthStatus.UNHEALTHY);
            } else {
                health.setStatus(LinkHealth.HealthStatus.DEGRADED);
            }
        }

        healthRepository.save(health);
        log.debug("Updated health status for link {}: {}", shortLinkId, health.getStatus());
    }

    @Override
    @Transactional
    public void resetHealth(Long shortLinkId) {
        log.info("Resetting health for link {}", shortLinkId);
        healthRepository.findByShortLinkId(shortLinkId).ifPresent(health -> {
            health.setStatus(LinkHealth.HealthStatus.UNKNOWN);
            health.setConsecutiveFailures(0);
            healthRepository.save(health);
        });
    }

    private LinkHealth createDefaultHealth(Long shortLinkId) {
        ShortLink shortLink = shortLinkRepository.findById(shortLinkId)
                .orElseThrow(() -> new ResourceNotFoundException("Short link not found"));

        return LinkHealth.builder()
                .shortLink(shortLink)
                .status(LinkHealth.HealthStatus.UNKNOWN)
                .consecutiveFailures(0)
                .checkCount(0L)
                .successCount(0L)
                .build();
    }

    private LinkHealthResponse toResponse(LinkHealth health) {
        return LinkHealthResponse.builder()
                .id(health.getId())
                .shortLinkId(health.getShortLink().getId())
                .status(health.getStatus().name())
                .lastStatusCode(health.getLastStatusCode())
                .lastResponseTimeMs(health.getLastResponseTimeMs())
                .lastError(health.getLastError())
                .consecutiveFailures(health.getConsecutiveFailures())
                .checkCount(health.getCheckCount())
                .successCount(health.getSuccessCount())
                .uptimePercentage(health.getUptimePercentage())
                .lastCheckedAt(health.getLastCheckedAt())
                .createdAt(health.getCreatedAt())
                .build();
    }
}
