package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for link password protection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Password protection response")
public class LinkPasswordResponse {

    @Schema(description = "Password protection ID", example = "1")
    private Long id;

    @Schema(description = "Short link ID", example = "100")
    @JsonProperty("short_link_id")
    private Long shortLinkId;

    @Schema(description = "Number of failed attempts", example = "0")
    @JsonProperty("failed_attempts")
    private Integer failedAttempts;

    @Schema(description = "Whether link is currently locked", example = "false")
    private Boolean locked;

    @Schema(description = "When lock expires")
    @JsonProperty("locked_until")
    private LocalDateTime lockedUntil;

    @Schema(description = "When password was created")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
