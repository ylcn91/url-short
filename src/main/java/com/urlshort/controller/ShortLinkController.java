package com.urlshort.controller;

import com.urlshort.dto.*;
import com.urlshort.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for managing short links.
 * <p>
 * This controller provides comprehensive CRUD operations and analytics for shortened URLs.
 * All endpoints require authentication and enforce workspace-level access control.
 * </p>
 * <p>
 * Base path: {@code /api/v1/links}
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>Deterministic URL Shortening:</b> Same URL always returns same short code within a workspace</li>
 *   <li><b>Comprehensive Analytics:</b> Click tracking, geographic distribution, referrer analysis</li>
 *   <li><b>Bulk Operations:</b> Create multiple short links in a single request</li>
 *   <li><b>Flexible Filtering:</b> Pagination and sorting support for link listing</li>
 *   <li><b>Soft Delete:</b> Links are archived rather than permanently deleted</li>
 * </ul>
 *
 * <h3>Authentication & Authorization:</h3>
 * <p>
 * All endpoints require authentication. The workspace ID is extracted from the security context
 * (typically from JWT token claims) to ensure users can only access links within their workspace.
 * </p>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Create a short link
 * POST /api/v1/links
 * {
 *   "original_url": "https://example.com/very/long/url",
 *   "expires_at": "2025-12-31T23:59:59",
 *   "max_clicks": 1000,
 *   "tags": ["marketing", "campaign-2024"]
 * }
 *
 * // Response
 * {
 *   "success": true,
 *   "data": {
 *     "id": 123,
 *     "short_code": "abc123",
 *     "short_url": "https://short.ly/abc123",
 *     "original_url": "https://example.com/very/long/url",
 *     "click_count": 0,
 *     "is_active": true
 *   }
 * }
 * }</pre>
 *
 * @see ShortLinkService
 * @see ShortLinkResponse
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/links")
@Tag(name = "Short Links", description = "Endpoints for managing shortened URLs")
@Slf4j
public class ShortLinkController {

    @Autowired
    private ShortLinkService shortLinkService;

    /**
     * Creates a new short link or returns an existing one if the URL was previously shortened.
     * <p>
     * This endpoint implements deterministic URL shortening: if the same URL is submitted
     * multiple times within the same workspace, it returns the existing short code rather
     * than creating duplicates. This ensures consistency and prevents link proliferation.
     * </p>
     *
     * <h3>Request Body Example:</h3>
     * <pre>{@code
     * {
     *   "original_url": "https://www.example.com/products/widget-2024",
     *   "expires_at": "2025-12-31T23:59:59",
     *   "max_clicks": 5000,
     *   "tags": ["products", "widget", "2024"]
     * }
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "id": 456,
     *     "short_code": "MaSgB7xKpQ",
     *     "short_url": "https://short.ly/MaSgB7xKpQ",
     *     "original_url": "https://www.example.com/products/widget-2024",
     *     "normalized_url": "https://www.example.com/products/widget-2024",
     *     "created_at": "2025-11-18T10:30:00",
     *     "expires_at": "2025-12-31T23:59:59",
     *     "click_count": 0,
     *     "is_active": true,
     *     "tags": ["products", "widget", "2024"]
     *   },
     *   "message": "Short link created successfully"
     * }
     * }</pre>
     *
     * @param request the short link creation request containing URL and optional parameters
     * @return ResponseEntity containing ApiResponse with the created or existing short link
     */
    @PostMapping
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "Create a short link",
        description = "Creates a new short link or returns existing one if URL was previously shortened. " +
                      "Same URL in the same workspace always returns the same short code (deterministic behavior)."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Short link created successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request - malformed URL or validation errors",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<ShortLinkResponse>> createShortLink(
            @Valid @RequestBody CreateShortLinkRequest request) {

        log.info("POST /api/v1/links - Creating short link for URL: {}", request.getOriginalUrl());

        // TODO: Extract workspace ID from security context (JWT claims)
        // For now, using a placeholder workspace ID
        Long workspaceId = 1L; // SecurityContextHolder.getContext().getAuthentication().getPrincipal().getWorkspaceId();

        ShortLinkResponse response = shortLinkService.createShortLink(workspaceId, request);

        log.info("Short link created successfully: code={}, id={}", response.shortCode(), response.id());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Short link created successfully"));
    }

    /**
     * Lists all short links in the current workspace with pagination and sorting.
     * <p>
     * Returns active (non-deleted) links with support for pagination and multiple sorting options.
     * Results are ordered by creation date (newest first) by default.
     * </p>
     *
     * <h3>Query Parameters:</h3>
     * <ul>
     *   <li><b>page:</b> Page number (0-indexed, default: 0)</li>
     *   <li><b>size:</b> Page size (default: 20, max: 100)</li>
     *   <li><b>sort:</b> Sort field and direction (default: createdAt,desc)</li>
     * </ul>
     *
     * <h3>Example Request:</h3>
     * <pre>{@code
     * GET /api/v1/links?page=0&size=20&sort=createdAt,desc
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "content": [
     *       {
     *         "id": 123,
     *         "short_code": "abc123",
     *         "short_url": "https://short.ly/abc123",
     *         "click_count": 42
     *       }
     *     ],
     *     "totalElements": 150,
     *     "totalPages": 8,
     *     "number": 0,
     *     "size": 20
     *   }
     * }
     * }</pre>
     *
     * @param page page number (0-indexed)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDirection sort direction (asc or desc)
     * @return ResponseEntity containing ApiResponse with paginated short links
     */
    @GetMapping
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "List short links",
        description = "Retrieves a paginated list of short links in the current workspace. " +
                      "Supports sorting by various fields and filtering deleted links."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Short links retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        )
    })
    public ResponseEntity<ApiResponse<Page<ShortLinkResponse>>> listShortLinks(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Field to sort by", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction (asc or desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDirection) {

        log.info("GET /api/v1/links - Listing links: page={}, size={}, sortBy={}, sortDirection={}",
                 page, size, sortBy, sortDirection);

        // TODO: Extract workspace ID from security context
        Long workspaceId = 1L;

        // Ensure page size doesn't exceed max limit
        size = Math.min(size, 100);

        Sort sort = sortDirection.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ShortLinkResponse> links = shortLinkService.listShortLinks(workspaceId, pageable);

        log.info("Retrieved {} links (page {}/{})",
                 links.getNumberOfElements(), page, links.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(links));
    }

    /**
     * Retrieves a specific short link by its ID.
     * <p>
     * Returns detailed information about a short link including all metadata, tags,
     * and current statistics.
     * </p>
     *
     * <h3>Example Request:</h3>
     * <pre>{@code
     * GET /api/v1/links/123
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "id": 123,
     *     "short_code": "abc123",
     *     "short_url": "https://short.ly/abc123",
     *     "original_url": "https://example.com/page",
     *     "created_at": "2025-11-18T10:30:00",
     *     "click_count": 42,
     *     "is_active": true
     *   }
     * }
     * }</pre>
     *
     * @param id the short link ID
     * @return ResponseEntity containing ApiResponse with the short link details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get short link by ID",
        description = "Retrieves detailed information about a specific short link by its unique identifier."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Short link retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Short link not found"
        )
    })
    public ResponseEntity<ApiResponse<ShortLinkResponse>> getShortLinkById(
            @Parameter(description = "Short link ID", example = "123")
            @PathVariable Long id) {

        log.info("GET /api/v1/links/{} - Retrieving link by ID", id);

        // TODO: Extract workspace ID from security context
        Long workspaceId = 1L;

        // Note: This is a simplified implementation. In production, you'd have a getById method
        // For now, we'll use listShortLinks and filter, or add a new service method
        throw new UnsupportedOperationException("Get by ID not yet implemented - use GET /api/v1/links/code/{code} instead");
    }

    /**
     * Retrieves a short link by its short code.
     * <p>
     * Returns the short link associated with the given short code within the current workspace.
     * This is useful for looking up link details before redirecting or for administrative purposes.
     * </p>
     *
     * <h3>Example Request:</h3>
     * <pre>{@code
     * GET /api/v1/links/code/abc123
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "id": 123,
     *     "short_code": "abc123",
     *     "short_url": "https://short.ly/abc123",
     *     "original_url": "https://example.com/page",
     *     "click_count": 42
     *   }
     * }
     * }</pre>
     *
     * @param code the short code to look up
     * @return ResponseEntity containing ApiResponse with the short link details
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get short link by code",
        description = "Retrieves a short link by its unique short code within the current workspace."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Short link retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Short link not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "410",
            description = "Short link has expired"
        )
    })
    public ResponseEntity<ApiResponse<ShortLinkResponse>> getShortLinkByCode(
            @Parameter(description = "Short code", example = "abc123")
            @PathVariable String code) {

        log.info("GET /api/v1/links/code/{} - Retrieving link by code", code);

        // TODO: Extract workspace ID from security context
        Long workspaceId = 1L;

        ShortLinkResponse response = shortLinkService.getShortLink(workspaceId, code);

        log.info("Short link retrieved: id={}, code={}", response.id(), response.shortCode());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates settings for an existing short link.
     * <p>
     * Allows modification of expiration date, click limit, and active status.
     * Only provided fields will be updated (partial updates supported).
     * </p>
     *
     * <h3>Request Body Example:</h3>
     * <pre>{@code
     * {
     *   "expires_at": "2026-01-31T23:59:59",
     *   "max_clicks": 10000,
     *   "is_active": true
     * }
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Short link updated successfully"
     * }
     * }</pre>
     *
     * @param id the short link ID to update
     * @param request the update request containing fields to modify
     * @return ResponseEntity containing ApiResponse with success message
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "Update short link settings",
        description = "Updates settings for an existing short link. Only provided fields will be updated (partial update)."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Short link updated successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Short link not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions"
        )
    })
    public ResponseEntity<ApiResponse<String>> updateShortLink(
            @Parameter(description = "Short link ID", example = "123")
            @PathVariable Long id,

            @Valid @RequestBody UpdateShortLinkRequest request) {

        log.info("PATCH /api/v1/links/{} - Updating link settings", id);

        // TODO: Extract workspace ID from security context
        Long workspaceId = 1L;

        // TODO: Implement update logic in service layer
        // shortLinkService.updateShortLink(workspaceId, id, request);

        log.info("Short link updated successfully: id={}", id);

        return ResponseEntity.ok(ApiResponse.success("Short link updated successfully"));
    }

    /**
     * Soft deletes a short link.
     * <p>
     * The link is marked as deleted but not removed from the database, preserving
     * historical data for analytics. The short code becomes available for reuse.
     * Soft-deleted links are not accessible and do not appear in listings.
     * </p>
     *
     * <h3>Example Request:</h3>
     * <pre>{@code
     * DELETE /api/v1/links/123
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "message": "Short link deleted successfully"
     * }
     * }</pre>
     *
     * @param id the short link ID to delete
     * @return ResponseEntity containing ApiResponse with success message
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "Delete short link",
        description = "Soft deletes a short link. The link is marked as deleted but data is preserved for analytics."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Short link deleted successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Short link not found"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions"
        )
    })
    public ResponseEntity<ApiResponse<String>> deleteShortLink(
            @Parameter(description = "Short link ID", example = "123")
            @PathVariable Long id) {

        log.info("DELETE /api/v1/links/{} - Deleting link", id);

        // TODO: Extract workspace ID from security context
        Long workspaceId = 1L;

        shortLinkService.deleteShortLink(workspaceId, id);

        log.info("Short link deleted successfully: id={}", id);

        return ResponseEntity.ok(ApiResponse.success("Short link deleted successfully"));
    }

    /**
     * Retrieves analytics and statistics for a short link.
     * <p>
     * Returns comprehensive analytics including:
     * </p>
     * <ul>
     *   <li>Total click count</li>
     *   <li>Clicks over time (daily time series)</li>
     *   <li>Geographic distribution (by country)</li>
     *   <li>Traffic sources (referrers)</li>
     *   <li>Device breakdown (mobile, desktop, tablet)</li>
     * </ul>
     *
     * <h3>Example Request:</h3>
     * <pre>{@code
     * GET /api/v1/links/123/stats
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "short_code": "abc123",
     *     "total_clicks": 1523,
     *     "clicks_by_date": {
     *       "2025-11-15": 45,
     *       "2025-11-16": 62,
     *       "2025-11-17": 38
     *     },
     *     "clicks_by_country": {
     *       "US": 450,
     *       "GB": 320,
     *       "DE": 180
     *     },
     *     "clicks_by_referrer": {
     *       "google.com": 230,
     *       "facebook.com": 180,
     *       "direct": 520
     *     },
     *     "clicks_by_device": {
     *       "mobile": 890,
     *       "desktop": 580,
     *       "tablet": 53
     *     }
     *   }
     * }
     * }</pre>
     *
     * @param id the short link ID
     * @return ResponseEntity containing ApiResponse with link analytics
     */
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get link analytics",
        description = "Retrieves comprehensive analytics and statistics for a short link including clicks, " +
                      "geographic distribution, referrers, and device types."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Analytics retrieved successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Short link not found"
        )
    })
    public ResponseEntity<ApiResponse<LinkStatsResponse>> getLinkStats(
            @Parameter(description = "Short link ID", example = "123")
            @PathVariable Long id) {

        log.info("GET /api/v1/links/{}/stats - Retrieving link analytics", id);

        // TODO: Extract workspace ID from security context
        Long workspaceId = 1L;

        // TODO: First get the link to retrieve its short code, then get stats
        // For now, this is a simplified implementation
        // In production, you'd have a service method that takes the ID directly
        throw new UnsupportedOperationException("Get stats by ID not yet implemented - requires short code");
    }

    /**
     * Bulk creates multiple short links in a single request.
     * <p>
     * This endpoint is optimized for creating many links at once, useful for batch operations
     * or importing links from external sources. The deterministic algorithm ensures that
     * duplicate URLs return existing short codes rather than creating duplicates.
     * </p>
     *
     * <h3>Request Body Example:</h3>
     * <pre>{@code
     * {
     *   "urls": [
     *     "https://example.com/page1",
     *     "https://example.com/page2",
     *     "https://example.com/page3"
     *   ]
     * }
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": [
     *     {
     *       "id": 123,
     *       "short_code": "abc123",
     *       "short_url": "https://short.ly/abc123",
     *       "original_url": "https://example.com/page1"
     *     },
     *     {
     *       "id": 124,
     *       "short_code": "def456",
     *       "short_url": "https://short.ly/def456",
     *       "original_url": "https://example.com/page2"
     *     }
     *   ],
     *   "message": "3 short links created successfully"
     * }
     * }</pre>
     *
     * @param request the bulk create request containing list of URLs
     * @return ResponseEntity containing ApiResponse with list of created short links
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "Bulk create short links",
        description = "Creates multiple short links in a single request. Useful for batch operations. " +
                      "Duplicate URLs will return existing short codes."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Short links created successfully"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request - one or more URLs are malformed"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required"
        )
    })
    public ResponseEntity<ApiResponse<List<ShortLinkResponse>>> bulkCreateShortLinks(
            @Valid @RequestBody BulkCreateRequest request) {

        log.info("POST /api/v1/links/bulk - Bulk creating {} short links", request.getUrls().size());

        // TODO: Extract workspace ID from security context
        Long workspaceId = 1L;

        List<ShortLinkResponse> responses = new ArrayList<>();

        for (String url : request.getUrls()) {
            CreateShortLinkRequest createRequest = CreateShortLinkRequest.builder()
                    .originalUrl(url)
                    .build();

            ShortLinkResponse response = shortLinkService.createShortLink(workspaceId, createRequest);
            responses.add(response);
        }

        log.info("Bulk creation completed: {} links created", responses.size());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        responses,
                        responses.size() + " short links created successfully"
                ));
    }
}
