package com.urlshort.dto.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for health check result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Health check result")
public class HealthCheckResult {

    @Schema(description = "Short link ID", example = "100")
    @JsonProperty("short_link_id")
    private Long shortLinkId;

    @Schema(description = "Whether link is healthy", example = "true")
    private Boolean healthy;

    @Schema(description = "HTTP status code", example = "200")
    @JsonProperty("status_code")
    private Integer statusCode;

    @Schema(description = "Response time in milliseconds", example = "234")
    @JsonProperty("response_time_ms")
    private Long responseTimeMs;

    @Schema(description = "Error message if check failed")
    private String error;

    @Schema(description = "When check was performed")
    @JsonProperty("checked_at")
    private LocalDateTime checkedAt;

    /**
     * Check if link is healthy.
     */
    public boolean isHealthy() {
        return Boolean.TRUE.equals(healthy);
    }
}
