package com.urlshort.controller;

import com.urlshort.dto.common.ApiResponse;
import com.urlshort.dto.workspace.AddMemberRequest;
import com.urlshort.dto.workspace.MemberResponse;
import com.urlshort.dto.workspace.UpdateWorkspaceRequest;
import com.urlshort.dto.workspace.WorkspaceResponse;
import com.urlshort.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces", description = "Endpoints for managing workspaces and members")
@Slf4j
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/current")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "Get current workspace")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getCurrentWorkspace() {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getCurrentWorkspace()));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update workspace settings")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.updateWorkspace(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Workspace updated successfully"));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(summary = "List workspace members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> listMembers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        size = Math.min(size, 100);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(workspaceService.listMembers(id, pageable)));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add workspace member")
    public ResponseEntity<ApiResponse<MemberResponse>> addMember(
            @PathVariable Long id,
            @Valid @RequestBody AddMemberRequest request) {
        MemberResponse response = workspaceService.addMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Member added successfully"));
    }
}
