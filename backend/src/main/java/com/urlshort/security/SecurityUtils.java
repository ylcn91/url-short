package com.urlshort.security;

import com.urlshort.domain.UserRole;
import com.urlshort.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility class for extracting authentication and authorization information
 * from the Spring Security context.
 * <p>
 * This class provides convenient methods to:
 * - Extract the currently authenticated user
 * - Get workspace ID from JWT claims
 * - Get user ID from JWT claims
 * - Check user roles and permissions
 * </p>
 * <p>
 * All methods are static and thread-safe as they work with the thread-local
 * SecurityContext.
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @RestController
 * public class MyController {
 *
 *     @PostMapping("/links")
 *     public ResponseEntity<...> createLink(@RequestBody CreateLinkRequest request) {
 *         Long workspaceId = SecurityUtils.getWorkspaceIdFromAuth();
 *         Long userId = SecurityUtils.getUserIdFromAuth();
 *
 *         // Use workspaceId and userId for business logic
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.1
 */
@Slf4j
public final class SecurityUtils {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     */
    private SecurityUtils() {
        throw new UnsupportedOperationException("SecurityUtils is a utility class and cannot be instantiated");
    }

    /**
     * Extracts the workspace ID from the current authentication context.
     * <p>
     * This method retrieves the workspace ID from the authenticated user's details.
     * The workspace ID is embedded in the JWT token during authentication and
     * represents the multi-tenant workspace context for the current request.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * Long workspaceId = SecurityUtils.getWorkspaceIdFromAuth();
     * // Use workspaceId for database queries, authorization checks, etc.
     * }</pre>
     *
     * @return the workspace ID of the currently authenticated user
     * @throws UnauthorizedException if user is not authenticated or workspace ID is not available
     */
    public static Long getWorkspaceIdFromAuth() {
        CustomUserDetails userDetails = getCurrentUserDetails()
            .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        Long workspaceId = userDetails.getWorkspaceId();

        if (workspaceId == null) {
            log.error("Workspace ID is null for authenticated user: {}", userDetails.getEmail());
            throw new UnauthorizedException("Workspace context not available for current user");
        }

        log.trace("Extracted workspace ID {} for user {}", workspaceId, userDetails.getEmail());
        return workspaceId;
    }

    /**
     * Extracts the user ID from the current authentication context.
     * <p>
     * This method retrieves the user ID from the authenticated user's details.
     * The user ID is embedded in the JWT token and represents the unique
     * identifier of the currently logged-in user.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * Long userId = SecurityUtils.getUserIdFromAuth();
     * // Use userId for audit logs, user-specific operations, etc.
     * }</pre>
     *
     * @return the ID of the currently authenticated user
     * @throws UnauthorizedException if user is not authenticated or user ID is not available
     */
    public static Long getUserIdFromAuth() {
        CustomUserDetails userDetails = getCurrentUserDetails()
            .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        Long userId = userDetails.getId();

        if (userId == null) {
            log.error("User ID is null for authenticated user: {}", userDetails.getEmail());
            throw new UnauthorizedException("User ID not available");
        }

        log.trace("Extracted user ID {} for user {}", userId, userDetails.getEmail());
        return userId;
    }

    /**
     * Gets the email of the currently authenticated user.
     *
     * @return the email address
     * @throws UnauthorizedException if user is not authenticated
     */
    public static String getCurrentUserEmail() {
        CustomUserDetails userDetails = getCurrentUserDetails()
            .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        return userDetails.getEmail();
    }

    /**
     * Gets the full name of the currently authenticated user.
     *
     * @return the full name
     * @throws UnauthorizedException if user is not authenticated
     */
    public static String getCurrentUserFullName() {
        CustomUserDetails userDetails = getCurrentUserDetails()
            .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        return userDetails.getFullName();
    }

    /**
     * Gets the role of the currently authenticated user.
     *
     * @return the user role
     * @throws UnauthorizedException if user is not authenticated
     */
    public static UserRole getCurrentUserRole() {
        CustomUserDetails userDetails = getCurrentUserDetails()
            .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        return userDetails.getRole();
    }

    /**
     * Checks if the currently authenticated user is an admin.
     *
     * @return true if user has ADMIN role, false otherwise
     */
    public static boolean isCurrentUserAdmin() {
        return getCurrentUserDetails()
            .map(CustomUserDetails::isAdmin)
            .orElse(false);
    }

    /**
     * Checks if the currently authenticated user is a member or higher (MEMBER or ADMIN).
     *
     * @return true if user has MEMBER or ADMIN role, false otherwise
     */
    public static boolean isCurrentUserMemberOrHigher() {
        return getCurrentUserDetails()
            .map(CustomUserDetails::isMemberOrHigher)
            .orElse(false);
    }

    /**
     * Checks if the currently authenticated user is a viewer.
     *
     * @return true if user has VIEWER role, false otherwise
     */
    public static boolean isCurrentUserViewer() {
        return getCurrentUserDetails()
            .map(CustomUserDetails::isViewer)
            .orElse(false);
    }

    /**
     * Gets the current user details if available.
     * <p>
     * This method safely extracts CustomUserDetails from the SecurityContext
     * without throwing exceptions if the user is not authenticated.
     * </p>
     *
     * @return Optional containing CustomUserDetails if authenticated, empty otherwise
     */
    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.trace("No authentication found in SecurityContext");
            return Optional.empty();
        }

        if (!authentication.isAuthenticated()) {
            log.trace("Authentication present but not authenticated");
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails)) {
            log.trace("Principal is not CustomUserDetails: {}", principal.getClass().getSimpleName());
            return Optional.empty();
        }

        return Optional.of((CustomUserDetails) principal);
    }

    /**
     * Checks if there is a currently authenticated user.
     *
     * @return true if a user is authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        return getCurrentUserDetails().isPresent();
    }

    /**
     * Validates that the current user belongs to the specified workspace.
     * <p>
     * This method is useful for authorization checks to ensure users can only
     * access resources within their own workspace.
     * </p>
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * // Verify user has access to this workspace before proceeding
     * SecurityUtils.requireWorkspaceAccess(workspaceId);
     * }</pre>
     *
     * @param workspaceId the workspace ID to validate
     * @throws UnauthorizedException if user doesn't belong to the specified workspace
     */
    public static void requireWorkspaceAccess(Long workspaceId) {
        Long userWorkspaceId = getWorkspaceIdFromAuth();

        if (!userWorkspaceId.equals(workspaceId)) {
            log.warn(
                "Access denied: User {} attempted to access workspace {} but belongs to workspace {}",
                getCurrentUserEmail(), workspaceId, userWorkspaceId
            );
            throw new UnauthorizedException(
                "Access denied: You do not have permission to access this workspace"
            );
        }
    }

    /**
     * Validates that the current user has admin role.
     *
     * @throws UnauthorizedException if user is not an admin
     */
    public static void requireAdminRole() {
        if (!isCurrentUserAdmin()) {
            log.warn(
                "Access denied: User {} with role {} attempted admin-only operation",
                getCurrentUserEmail(), getCurrentUserRole()
            );
            throw new UnauthorizedException("Admin role required for this operation");
        }
    }

    /**
     * Validates that the current user has at least member role.
     *
     * @throws UnauthorizedException if user is only a viewer
     */
    public static void requireMemberOrHigherRole() {
        if (!isCurrentUserMemberOrHigher()) {
            log.warn(
                "Access denied: User {} with role {} attempted member-level operation",
                getCurrentUserEmail(), getCurrentUserRole()
            );
            throw new UnauthorizedException("Member or Admin role required for this operation");
        }
    }
}
