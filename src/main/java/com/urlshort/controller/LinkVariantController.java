package com.urlshort.controller;

import com.urlshort.dto.*;
import com.urlshort.service.LinkVariantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for A/B testing variants.
 * Base path: /api/v1/links/{linkId}/variants
 */
@RestController
@RequestMapping("/api/v1/links/{linkId}/variants")
@Tag(name = "A/B Testing", description = "Endpoints for A/B testing and link variants")
@Slf4j
public class LinkVariantController {

    @Autowired
    private LinkVariantService variantService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Create A/B test variant",
               description = "Create a new variant for A/B testing. Total weight of all variants must not exceed 100%.")
    public ResponseEntity<ApiResponse<LinkVariantResponse>> createVariant(
            @Parameter(description = "Short link ID") @PathVariable Long linkId,
            @Valid @RequestBody LinkVariantRequest request) {
        log.info("Creating variant {} for link {}", request.getName(), linkId);
        LinkVariantResponse response = variantService.createVariant(linkId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Update variant",
               description = "Update an A/B test variant configuration")
    public ResponseEntity<ApiResponse<LinkVariantResponse>> updateVariant(
            @Parameter(description = "Short link ID") @PathVariable Long linkId,
            @Parameter(description = "Variant ID") @PathVariable Long variantId,
            @Valid @RequestBody LinkVariantRequest request) {
        log.info("Updating variant {}", variantId);
        LinkVariantResponse response = variantService.updateVariant(variantId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Get all variants",
               description = "Retrieve all variants for a short link")
    public ResponseEntity<ApiResponse<List<LinkVariantResponse>>> getVariants(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        log.info("Getting variants for link {}", linkId);
        List<LinkVariantResponse> response = variantService.getVariants(linkId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Get A/B test statistics",
               description = "Retrieve comprehensive A/B test statistics including conversion rates")
    public ResponseEntity<ApiResponse<VariantStatsResponse>> getVariantStats(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        log.info("Getting variant stats for link {}", linkId);
        VariantStatsResponse response = variantService.getVariantStats(linkId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{variantId}/conversion")
    @Operation(summary = "Record conversion",
               description = "Record a conversion event for a variant")
    public ResponseEntity<ApiResponse<Void>> recordConversion(
            @Parameter(description = "Short link ID") @PathVariable Long linkId,
            @Parameter(description = "Variant ID") @PathVariable Long variantId) {
        log.info("Recording conversion for variant {}", variantId);
        variantService.recordConversion(variantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Delete variant",
               description = "Delete an A/B test variant")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @Parameter(description = "Short link ID") @PathVariable Long linkId,
            @Parameter(description = "Variant ID") @PathVariable Long variantId) {
        log.info("Deleting variant {}", variantId);
        variantService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/deactivate-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Deactivate all variants",
               description = "Deactivate all A/B test variants for a link")
    public ResponseEntity<ApiResponse<Void>> deactivateAllVariants(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        log.info("Deactivating all variants for link {}", linkId);
        variantService.deactivateAllVariants(linkId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
