package com.urlshort.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user signup request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;

    @NotBlank(message = "Workspace name is required")
    @Size(max = 255, message = "Workspace name must not exceed 255 characters")
    private String workspaceName;

    @NotBlank(message = "Workspace slug is required")
    @Size(max = 100, message = "Workspace slug must not exceed 100 characters")
    @Pattern(
        regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
        message = "Slug must be lowercase alphanumeric with hyphens, starting and ending with alphanumeric"
    )
    private String workspaceSlug;
}
