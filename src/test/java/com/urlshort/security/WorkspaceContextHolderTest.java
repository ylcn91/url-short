package com.urlshort.security;

import com.urlshort.domain.User;
import com.urlshort.domain.UserRole;
import com.urlshort.domain.Workspace;
import com.urlshort.exception.ForbiddenAccessException;
import com.urlshort.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WorkspaceContextHolder Unit Tests")
class WorkspaceContextHolderTest {

    private Workspace workspace;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(1L)
                .name("Test Workspace")
                .slug("test-ws")
                .isDeleted(false)
                .settings(new HashMap<>())
                .build();

        user = User.builder()
                .id(10L)
                .workspace(workspace)
                .email("user@example.com")
                .fullName("Test User")
                .passwordHash("hashed")
                .role(UserRole.ADMIN)
                .isDeleted(false)
                .build();

        userDetails = new CustomUserDetails(user);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(Object principal) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("getCurrentUser returns CustomUserDetails when authenticated")
    void getCurrentUser_validAuth_returnsUserDetails() {
        setAuthentication(userDetails);

        CustomUserDetails result = WorkspaceContextHolder.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getWorkspaceId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getFullName()).isEqualTo("Test User");
        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("getCurrentUser throws UnauthorizedException when no authentication present")
    void getCurrentUser_noAuth_throwsUnauthorizedException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(WorkspaceContextHolder::getCurrentUser)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    @DisplayName("getCurrentUser throws UnauthorizedException when principal is not CustomUserDetails")
    void getCurrentUser_invalidPrincipal_throwsUnauthorizedException() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("string-principal", null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(WorkspaceContextHolder::getCurrentUser)
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("No authenticated user found");
    }

    @Test
    @DisplayName("getCurrentWorkspaceId returns workspace ID from authenticated user")
    void getCurrentWorkspaceId_returnsWorkspaceId() {
        setAuthentication(userDetails);

        Long workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();

        assertThat(workspaceId).isEqualTo(1L);
    }

    @Test
    @DisplayName("getCurrentUserId returns user ID from authenticated user")
    void getCurrentUserId_returnsUserId() {
        setAuthentication(userDetails);

        Long userId = WorkspaceContextHolder.getCurrentUserId();

        assertThat(userId).isEqualTo(10L);
    }

    @Test
    @DisplayName("verifyWorkspaceAccess succeeds when workspace IDs match")
    void verifyWorkspaceAccess_matchingId_succeeds() {
        setAuthentication(userDetails);

        // Should not throw
        WorkspaceContextHolder.verifyWorkspaceAccess(1L);
    }

    @Test
    @DisplayName("verifyWorkspaceAccess throws ForbiddenAccessException when workspace IDs do not match")
    void verifyWorkspaceAccess_mismatchedId_throwsForbiddenAccessException() {
        setAuthentication(userDetails);

        assertThatThrownBy(() -> WorkspaceContextHolder.verifyWorkspaceAccess(999L))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessageContaining("Access denied to workspace 999");
    }
}
