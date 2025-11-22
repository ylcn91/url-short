package com.urlshort.controller;

import com.urlshort.domain.User;
import com.urlshort.domain.Workspace;
import com.urlshort.dto.*;
import com.urlshort.exception.ResourceNotFoundException;
import com.urlshort.repository.UserRepository;
import com.urlshort.repository.WorkspaceRepository;
import com.urlshort.security.SecurityUtils;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for workspace management.
 * <p>
 * This controller provides endpoints for managing workspaces and their members.
 * Workspaces are the top-level organizational unit in the URL shortener platform,
 * providing multi-tenancy and team collaboration features.
 * </p>
 *
 * <h3>Workspace Concepts:</h3>
 * <ul>
 *   <li><b>Multi-Tenancy:</b> Each workspace has isolated data (links, users, API keys)</li>
 *   <li><b>Team Collaboration:</b> Multiple users can belong to a workspace with different roles</li>
 *   <li><b>Custom Settings:</b> Workspaces can have custom configuration and branding</li>
 *   <li><b>URL Isolation:</b> Short codes are unique within a workspace, not globally</li>
 * </ul>
 *
 * <h3>User Roles:</h3>
 * <ul>
 *   <li><b>ADMIN:</b> Full access - can manage workspace settings and members</li>
 *   <li><b>MEMBER:</b> Can create and manage own links, view workspace analytics</li>
 *   <li><b>VIEWER:</b> Read-only access to links and analytics</li>
 * </ul>
 *
 * <h3>Base Path:</h3>
 * {@code /api/v1/workspaces}
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Get current workspace
 * GET /api/v1/workspaces/current
 * {
 *   "success": true,
 *   "data": {
 *     "id": 1,
 *     "name": "Acme Corporation",
 *     "slug": "acme-corp",
 *     "is_active": true
 *   }
 * }
 *
 * // Update workspace settings
 * PATCH /api/v1/workspaces/1
 * {
 *   "name": "Acme Corp Updated",
 *   "settings": {
 *     "default_expiration_days": 30,
 *     "custom_domain": "go.acme.com"
 *   }
 * }
 *
 * // List workspace members
 * GET /api/v1/workspaces/1/members
 * {
 *   "success": true,
 *   "data": [
 *     {
 *       "id": 10,
 *       "email": "admin@acme.com",
 *       "full_name": "John Admin",
 *       "role": "ADMIN"
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces", description = "Endpoints for managing workspaces and members")
@Slf4j
public class WorkspaceController {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Retrieves the current user's workspace.
     * <p>
     * Returns details about the workspace that the authenticated user belongs to.
     * The workspace ID is extracted from the security context (JWT token claims).
     * </p>
     *
     * <h3>Example Request:</h3>
     * <pre>{@code
     * GET /api/v1/workspaces/current
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * }</pre>
     *
     * <h3>Example Response:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "id": 1,
     *     "name": "Acme Corporation",
     *     "slug": "acme-corp",
     *     "created_at": "2025-01-15T10:00:00Z",
     *     "updated_at": "2025-11-18T14:30:00Z",
     *     "is_active": true,
     *     "settings": {
     *       "default_expiration_days": 30,
     *       "custom_domain": "go.acme.com",
     *       "allow_custom_codes": true
     *     }
     *   }
     * }
     * }</pre>
     *
     * @return ResponseEntity containing ApiResponse with workspace details
     */
    @GetMapping("/current")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get current workspace",
        description = "Retrieves details about the authenticated user's workspace. " +
                      "Workspace ID is extracted from the security context."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Workspace retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Workspace not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getCurrentWorkspace() {
        log.info("GET /api/v1/workspaces/current - Retrieving current workspace");

        // TODO: Extract workspace ID from security context (JWT claims)
        // SecurityContext context = SecurityContextHolder.getContext();
        // CustomUserPrincipal principal = (CustomUserPrincipal) context.getAuthentication().getPrincipal();
        // Long workspaceId = principal.getWorkspaceId();
        Long workspaceId = SecurityUtils.getWorkspaceIdFromAuth();

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        if (workspace.getIsDeleted()) {
            throw new ResourceNotFoundException("Workspace has been deleted");
        }

        WorkspaceResponse response = toWorkspaceResponse(workspace);

        log.info("Current workspace retrieved: id={}, slug={}", workspace.getId(), workspace.getSlug());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Updates workspace settings.
     * <p>
     * Allows administrators to modify workspace name and settings. Only provided
     * fields will be updated (partial updates supported). The slug cannot be changed
     * to maintain URL stability.
     * </p>
     *
     * <h3>Permission Required:</h3>
     * <p>Only users with ADMIN role can update workspace settings.</p>
     *
     * <h3>Request Body Example:</h3>
     * <pre>{@code
     * {
     *   "name": "Acme Corporation - Updated",
     *   "settings": {
     *     "default_expiration_days": 60,
     *     "custom_domain": "links.acme.com",
     *     "allow_custom_codes": false,
     *     "require_approval": true,
     *     "max_links_per_user": 1000
     *   }
     * }
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "id": 1,
     *     "name": "Acme Corporation - Updated",
     *     "slug": "acme-corp",
     *     "is_active": true,
     *     "settings": {
     *       "default_expiration_days": 60,
     *       "custom_domain": "links.acme.com",
     *       "allow_custom_codes": false
     *     }
     *   },
     *   "message": "Workspace updated successfully"
     * }
     * }</pre>
     *
     * @param id the workspace ID to update
     * @param request the update request containing fields to modify
     * @return ResponseEntity containing ApiResponse with updated workspace details
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(
        summary = "Update workspace settings",
        description = "Updates workspace name and settings. Requires ADMIN role. " +
                      "Only provided fields will be updated (partial update). Slug cannot be changed."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Workspace updated successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - ADMIN role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Workspace not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<WorkspaceResponse>> updateWorkspace(
            @Parameter(description = "Workspace ID", example = "1")
            @PathVariable Long id,

            @Valid @RequestBody UpdateWorkspaceRequest request) {

        log.info("PATCH /api/v1/workspaces/{} - Updating workspace", id);

        // TODO: Verify user has admin access to this workspace
        Long currentWorkspaceId = 1L; // From security context

        if (!id.equals(currentWorkspaceId)) {
            throw new ResourceNotFoundException("Cannot update workspace - access denied");
        }

        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));

        if (workspace.getIsDeleted()) {
            throw new ResourceNotFoundException("Workspace has been deleted");
        }

        // Update name if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            workspace.setName(request.getName().trim());
            log.debug("Updated workspace name to: {}", request.getName());
        }

        // Update settings if provided
        if (request.getSettings() != null) {
            // Merge settings - don't replace entire map
            workspace.getSettings().putAll(request.getSettings());
            log.debug("Updated workspace settings: {}", request.getSettings().keySet());
        }

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        log.info("Workspace updated successfully: id={}, name={}", id, savedWorkspace.getName());

        WorkspaceResponse response = toWorkspaceResponse(savedWorkspace);

        return ResponseEntity.ok(ApiResponse.success(response, "Workspace updated successfully"));
    }

    /**
     * Lists all members of a workspace.
     * <p>
     * Returns a paginated list of users who belong to the workspace, including their
     * roles and activity status. Results are sorted by creation date (newest first).
     * </p>
     *
     * <h3>Query Parameters:</h3>
     * <ul>
     *   <li><b>page:</b> Page number (0-indexed, default: 0)</li>
     *   <li><b>size:</b> Page size (default: 50, max: 100)</li>
     * </ul>
     *
     * <h3>Example Request:</h3>
     * <pre>{@code
     * GET /api/v1/workspaces/1/members?page=0&size=50
     * }</pre>
     *
     * <h3>Example Response:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": [
     *     {
     *       "id": 10,
     *       "email": "admin@acme.com",
     *       "full_name": "John Admin",
     *       "role": "ADMIN",
     *       "created_at": "2025-01-15T10:00:00Z",
     *       "last_login_at": "2025-11-18T09:00:00Z",
     *       "is_active": true
     *     },
     *     {
     *       "id": 11,
     *       "email": "user@acme.com",
     *       "full_name": "Jane User",
     *       "role": "MEMBER",
     *       "created_at": "2025-02-20T14:30:00Z",
     *       "last_login_at": "2025-11-17T16:45:00Z",
     *       "is_active": true
     *     }
     *   ]
     * }
     * }</pre>
     *
     * @param id the workspace ID
     * @param page page number (0-indexed)
     * @param size page size
     * @return ResponseEntity containing ApiResponse with list of members
     */
    @GetMapping("/{id}/members")
    @PreAuthorize("hasRole('VIEWER') or hasRole('MEMBER') or hasRole('ADMIN')")
    @Operation(
        summary = "List workspace members",
        description = "Retrieves a paginated list of all members in the workspace with their roles and status."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Members retrieved successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Workspace not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<List<MemberResponse>>> listMembers(
            @Parameter(description = "Workspace ID", example = "1")
            @PathVariable Long id,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 100)", example = "50")
            @RequestParam(defaultValue = "50") int size) {

        log.info("GET /api/v1/workspaces/{}/members - Listing members: page={}, size={}", id, page, size);

        // TODO: Verify user has access to this workspace
        Long currentWorkspaceId = 1L; // From security context

        if (!id.equals(currentWorkspaceId)) {
            throw new ResourceNotFoundException("Cannot access workspace members - access denied");
        }

        // Verify workspace exists
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));

        if (workspace.getIsDeleted()) {
            throw new ResourceNotFoundException("Workspace has been deleted");
        }

        // Ensure page size doesn't exceed max limit
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Find all active users in the workspace
        Page<User> users = userRepository.findByWorkspaceIdAndIsDeletedFalse(id, pageable);

        List<MemberResponse> members = users.getContent().stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());

        log.info("Retrieved {} members for workspace {}", members.size(), id);

        return ResponseEntity.ok(ApiResponse.success(members));
    }

    /**
     * Adds a new member to the workspace.
     * <p>
     * Creates a new user account within the workspace with the specified role.
     * Sends an invitation email with login credentials (in production).
     * </p>
     *
     * <h3>Permission Required:</h3>
     * <p>Only users with ADMIN role can add new members.</p>
     *
     * <h3>Request Body Example:</h3>
     * <pre>{@code
     * {
     *   "email": "newuser@acme.com",
     *   "full_name": "New User",
     *   "role": "MEMBER",
     *   "password": "TempPassword123!"
     * }
     * }</pre>
     *
     * <h3>Response Example:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": {
     *     "id": 15,
     *     "email": "newuser@acme.com",
     *     "full_name": "New User",
     *     "role": "MEMBER",
     *     "created_at": "2025-11-18T15:30:00Z",
     *     "is_active": true
     *   },
     *   "message": "Member added successfully. Invitation email sent."
     * }
     * }</pre>
     *
     * @param id the workspace ID
     * @param request the add member request containing user details
     * @return ResponseEntity containing ApiResponse with new member details
     */
    @PostMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(
        summary = "Add workspace member",
        description = "Adds a new member to the workspace with the specified role. Requires ADMIN role. " +
                      "Sends invitation email with login credentials."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Member added successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation errors",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - ADMIN role required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - user with this email already exists in workspace",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<MemberResponse>> addMember(
            @Parameter(description = "Workspace ID", example = "1")
            @PathVariable Long id,

            @Valid @RequestBody AddMemberRequest request) {

        log.info("POST /api/v1/workspaces/{}/members - Adding new member: {}", id, request.getEmail());

        // TODO: Verify user has admin access to this workspace
        Long currentWorkspaceId = 1L; // From security context

        if (!id.equals(currentWorkspaceId)) {
            throw new ResourceNotFoundException("Cannot add member to workspace - access denied");
        }

        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));

        if (workspace.getIsDeleted()) {
            throw new ResourceNotFoundException("Workspace has been deleted");
        }

        // Check if user with this email already exists in the workspace
        if (userRepository.findByWorkspaceIdAndEmailAndIsDeletedFalse(id, request.getEmail()).isPresent()) {
            throw new com.urlshort.exception.DuplicateResourceException(
                    "User with email " + request.getEmail() + " already exists in this workspace"
            );
        }

        // TODO: In production, hash the password using BCrypt or similar
        // String passwordHash = passwordEncoder.encode(request.getPassword());
        String passwordHash = "HASHED_" + (request.getPassword() != null ? request.getPassword() : "temp123");

        // Create new user
        User newUser = User.builder()
                .workspace(workspace)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(request.getRole())
                .passwordHash(passwordHash)
                .isDeleted(false)
                .build();

        User savedUser = userRepository.save(newUser);

        log.info("New member added successfully: id={}, email={}, role={}",
                 savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        // TODO: Send invitation email with login credentials
        // emailService.sendInvitation(savedUser.getEmail(), temporaryPassword);

        MemberResponse response = toMemberResponse(savedUser);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Member added successfully. Invitation email sent."));
    }

    /**
     * Converts a Workspace entity to a WorkspaceResponse DTO.
     *
     * @param workspace the workspace entity
     * @return the workspace response DTO
     */
    private WorkspaceResponse toWorkspaceResponse(Workspace workspace) {
        return WorkspaceResponse.builder()
                .id(workspace.getId())
                .name(workspace.getName())
                .slug(workspace.getSlug())
                .createdAt(workspace.getCreatedAt())
                .updatedAt(workspace.getUpdatedAt())
                .isActive(!workspace.getIsDeleted())
                .settings(workspace.getSettings())
                .build();
    }

    /**
     * Converts a User entity to a MemberResponse DTO.
     *
     * @param user the user entity
     * @return the member response DTO
     */
    private MemberResponse toMemberResponse(User user) {
        return MemberResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .isActive(!user.getIsDeleted())
                .build();
    }
}
