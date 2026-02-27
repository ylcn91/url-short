package com.urlshort.controller;

import com.urlshort.dto.common.ApiResponse;
import com.urlshort.dto.link.BulkCreateRequest;
import com.urlshort.dto.link.CreateShortLinkRequest;
import com.urlshort.dto.link.LinkStatsResponse;
import com.urlshort.dto.link.ShortLinkResponse;
import com.urlshort.dto.link.UpdateShortLinkRequest;
import com.urlshort.security.WorkspaceContextHolder;
import com.urlshort.service.ShortLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/links")
@Tag(name = "Short Links", description = "Endpoints for managing shortened URLs")
@Slf4j
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Create a short link")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> createShortLink(
            @Valid @RequestBody CreateShortLinkRequest request) {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        ShortLinkResponse response = shortLinkService.createShortLink(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Short link created successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "List short links")
    public ResponseEntity<ApiResponse<Page<ShortLinkResponse>>> listShortLinks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        size = Math.min(size, 100);
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Page<ShortLinkResponse> links = shortLinkService.listShortLinks(workspaceId, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(links));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Get short link by ID")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> getShortLinkById(
            @Parameter(description = "Short link ID") @PathVariable Long id) {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        ShortLinkResponse response = shortLinkService.getShortLinkById(workspaceId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Get short link by code")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> getShortLinkByCode(
            @Parameter(description = "Short code") @PathVariable String code) {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        ShortLinkResponse response = shortLinkService.getShortLink(workspaceId, code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Update short link settings")
    public ResponseEntity<ApiResponse<ShortLinkResponse>> updateShortLink(
            @Parameter(description = "Short link ID") @PathVariable Long id,
            @Valid @RequestBody UpdateShortLinkRequest request) {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        ShortLinkResponse response = shortLinkService.updateShortLink(workspaceId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Short link updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Delete short link")
    public ResponseEntity<ApiResponse<String>> deleteShortLink(
            @Parameter(description = "Short link ID") @PathVariable Long id) {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        shortLinkService.deleteShortLink(workspaceId, id);
        return ResponseEntity.ok(ApiResponse.success("Short link deleted successfully"));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Get link analytics")
    public ResponseEntity<ApiResponse<LinkStatsResponse>> getLinkStats(
            @Parameter(description = "Short link ID") @PathVariable Long id) {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        LinkStatsResponse stats = shortLinkService.getLinkStatsById(workspaceId, id);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Bulk create short links")
    public ResponseEntity<ApiResponse<List<ShortLinkResponse>>> bulkCreateShortLinks(
            @Valid @RequestBody BulkCreateRequest request) {
        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        List<ShortLinkResponse> responses = shortLinkService.bulkCreateShortLinks(workspaceId, request.getUrls());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(responses, responses.size() + " short links created successfully"));
    }
}
