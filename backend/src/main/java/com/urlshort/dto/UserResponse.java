package com.urlshort.dto;

import com.urlshort.domain.User;
import com.urlshort.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for user information response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private UserRole role;
    private Long workspaceId;
    private String workspaceName;
    private String workspaceSlug;
    private Instant createdAt;
    private Instant lastLoginAt;

    /**
     * Creates a UserResponse from a User entity.
     *
     * @param user the user entity
     * @return UserResponse DTO
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .workspaceId(user.getWorkspace().getId())
            .workspaceName(user.getWorkspace().getName())
            .workspaceSlug(user.getWorkspace().getSlug())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}
