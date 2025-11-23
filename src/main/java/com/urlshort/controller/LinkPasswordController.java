package com.urlshort.controller;

import com.urlshort.dto.*;
import com.urlshort.service.LinkPasswordService;
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

/**
 * REST controller for password-protected links.
 * Base path: /api/v1/links/{linkId}/password
 */
@RestController
@RequestMapping("/api/v1/links/{linkId}/password")
@Tag(name = "Link Password", description = "Endpoints for password-protected links")
@Slf4j
public class LinkPasswordController {

    @Autowired
    private LinkPasswordService passwordService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Add password protection",
               description = "Add password protection to a short link")
    public ResponseEntity<ApiResponse<LinkPasswordResponse>> addPassword(
            @Parameter(description = "Short link ID") @PathVariable Long linkId,
            @Valid @RequestBody LinkPasswordRequest request) {
        log.info("Adding password protection to link {}", linkId);
        LinkPasswordResponse response = passwordService.addPassword(linkId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate password",
               description = "Validate password for a protected link. Returns access token if valid.")
    public ResponseEntity<ApiResponse<PasswordValidationResponse>> validatePassword(
            @Parameter(description = "Short link ID") @PathVariable Long linkId,
            @Valid @RequestBody PasswordValidationRequest request) {
        log.info("Validating password for link {}", linkId);
        PasswordValidationResponse response = passwordService.validatePassword(linkId, request);

        if (!response.getValid()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid password", null));
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status")
    @Operation(summary = "Check if link is password protected",
               description = "Check if a link has password protection enabled")
    public ResponseEntity<ApiResponse<Boolean>> isPasswordProtected(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        boolean protected_ = passwordService.isPasswordProtected(linkId);
        return ResponseEntity.ok(ApiResponse.success(protected_));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MEMBER')")
    @Operation(summary = "Remove password protection",
               description = "Remove password protection from a link")
    public ResponseEntity<ApiResponse<Void>> removePassword(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        log.info("Removing password protection from link {}", linkId);
        passwordService.removePassword(linkId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/reset-attempts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset failed attempts",
               description = "Reset failed password attempts for a locked link (Admin only)")
    public ResponseEntity<ApiResponse<Void>> resetFailedAttempts(
            @Parameter(description = "Short link ID") @PathVariable Long linkId) {
        log.info("Resetting failed attempts for link {}", linkId);
        passwordService.resetFailedAttempts(linkId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
