package com.urlshort.controller;

import com.urlshort.dto.common.ApiResponse;
import com.urlshort.dto.domain.CustomDomainRequest;
import com.urlshort.dto.domain.CustomDomainResponse;
import com.urlshort.dto.domain.DomainVerificationResponse;
import com.urlshort.service.CustomDomainService;
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
 * REST controller for custom domain management.
 * Base path: /api/v1/domains
 */
@RestController
@RequestMapping("/api/v1/domains")
@Tag(name = "Custom Domains", description = "Endpoints for managing custom branded domains")
@Slf4j
@RequiredArgsConstructor
public class CustomDomainController {

    private final CustomDomainService domainService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Register a new custom domain",
               description = "Register a custom domain for the workspace. Returns verification token for DNS setup.")
    public ResponseEntity<ApiResponse<CustomDomainResponse>> registerDomain(
            @Parameter(description = "Workspace ID") @RequestParam Long workspaceId,
            @Valid @RequestBody CustomDomainRequest request) {
        log.info("Registering domain {} for workspace {}", request.getDomain(), workspaceId);
        CustomDomainResponse response = domainService.registerDomain(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/{domainId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Verify domain ownership",
               description = "Verify domain ownership via DNS TXT record check")
    public ResponseEntity<ApiResponse<DomainVerificationResponse>> verifyDomain(
            @Parameter(description = "Domain ID") @PathVariable Long domainId) {
        log.info("Verifying domain {}", domainId);
        DomainVerificationResponse response = domainService.verifyDomain(domainId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{domainId}/set-default")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Set domain as default",
               description = "Set this domain as the default for the workspace")
    public ResponseEntity<ApiResponse<CustomDomainResponse>> setAsDefault(
            @Parameter(description = "Domain ID") @PathVariable Long domainId) {
        log.info("Setting domain {} as default", domainId);
        CustomDomainResponse response = domainService.setAsDefault(domainId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Get all domains for workspace",
               description = "Retrieve all custom domains for the workspace")
    public ResponseEntity<ApiResponse<List<CustomDomainResponse>>> getWorkspaceDomains(
            @Parameter(description = "Workspace ID") @RequestParam Long workspaceId) {
        log.info("Getting domains for workspace {}", workspaceId);
        List<CustomDomainResponse> response = domainService.getWorkspaceDomains(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{domain}")
    @Operation(summary = "Get domain by name",
               description = "Retrieve domain information by domain name")
    public ResponseEntity<ApiResponse<CustomDomainResponse>> getDomainByName(
            @Parameter(description = "Domain name") @PathVariable String domain) {
        log.info("Getting domain {}", domain);
        CustomDomainResponse response = domainService.getDomainByName(domain);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{domainId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete custom domain",
               description = "Delete a custom domain (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteDomain(
            @Parameter(description = "Domain ID") @PathVariable Long domainId) {
        log.info("Deleting domain {}", domainId);
        domainService.deleteDomain(domainId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
