package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.urlshort.domain.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a new member to a workspace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for adding a new workspace member")
public class AddMemberRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Member's email address", example = "newuser@example.com")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    @Schema(description = "Member's full name", example = "Jane Smith")
    @JsonProperty("full_name")
    private String fullName;

    @NotNull(message = "Role is required")
    @Schema(description = "Member's role in the workspace", example = "MEMBER")
    private UserRole role;

    @Schema(description = "Temporary password for the new member (will be sent via email)", example = "TempPass123!")
    private String password;
}
