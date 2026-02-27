package com.urlshort.security;

import com.urlshort.exception.ForbiddenAccessException;
import com.urlshort.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class WorkspaceContextHolder {

    private WorkspaceContextHolder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new UnauthorizedException("No authenticated user found");
        }
        return (CustomUserDetails) authentication.getPrincipal();
    }

    public static Long getCurrentWorkspaceId() {
        return getCurrentUser().getWorkspaceId();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static void verifyWorkspaceAccess(Long requestedWorkspaceId) {
        Long currentWorkspaceId = getCurrentWorkspaceId();
        if (!currentWorkspaceId.equals(requestedWorkspaceId)) {
            throw new ForbiddenAccessException(
                    "Access denied to workspace " + requestedWorkspaceId);
        }
    }
}
