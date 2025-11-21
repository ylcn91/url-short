package com.urlshort.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for validating a password-protected link.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for password validation")
public class PasswordValidationRequest {

    @NotBlank(message = "Password is required")
    @Schema(description = "Password to validate", example = "secret123")
    private String password;
}
