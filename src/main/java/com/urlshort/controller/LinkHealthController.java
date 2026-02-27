package com.urlshort.controller;

import com.urlshort.dto.common.ApiResponse;
import com.urlshort.dto.health.HealthCheckResult;
import com.urlshort.dto.health.LinkHealthResponse;
import com.urlshort.service.LinkHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for link health monitoring.
 * Base path: /api/v1/links/{linkId}/health
 */
@RestController
@RequestMapping("/api/v1/links/{linkId}/health")
@Tag(name = "Link Health", description = "Endpoints for link health monitoring and uptime tracking")
@Slf4j
@RequiredArgsConstructor
public class LinkHealthController {

    private final LinkHealthService healthService;

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Perform health check",
               description = "Manually trigger a health check for a short link")
    public ResponseEntity<ApiResponse<HealthCheckResult>> performHealthCheck(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        log.info("Performing health check for link {}", linkId);
        HealthCheckResult result = healthService.performHealthCheck(linkId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Get health status",
               description = "Retrieve health status and metrics for a short link")
    public ResponseEntity<ApiResponse<LinkHealthResponse>> getHealth(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        log.info("Getting health for link {}", linkId);
        LinkHealthResponse response = healthService.getHealth(linkId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/workspace/{workspaceId}/unhealthy")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Get unhealthy links",
               description = "Retrieve all unhealthy links for a workspace")
    public ResponseEntity<ApiResponse<List<LinkHealthResponse>>> getUnhealthyLinks(
            @Parameter(description = "Short link ID") @PathVariable Long linkId,
            @Parameter(description = "Workspace ID") @PathVariable Long workspaceId) {
        log.info("Getting unhealthy links for workspace {}", workspaceId);
        List<LinkHealthResponse> response = healthService.getUnhealthyLinks(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset health status",
               description = "Reset health status for a link (Admin only)")
    public ResponseEntity<ApiResponse<Void>> resetHealth(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        log.info("Resetting health for link {}", linkId);
        healthService.resetHealth(linkId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
