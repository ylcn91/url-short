package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for password validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password validation result")
public class PasswordValidationResponse {

    @Schema(description = "Whether password is valid", example = "true")
    private Boolean valid;

    @Schema(description = "Whether link is locked", example = "false")
    private Boolean locked;

    @Schema(description = "When lock expires")
    @JsonProperty("locked_until")
    private LocalDateTime lockedUntil;

    @Schema(description = "Number of failed attempts")
    @JsonProperty("failed_attempts")
    private Integer failedAttempts;

    @Schema(description = "Access token if validation succeeded")
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "Validation message")
    private String message;
}
