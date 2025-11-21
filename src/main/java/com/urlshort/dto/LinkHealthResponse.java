package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for link health status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Link health status response")
public class LinkHealthResponse {

    @Schema(description = "Health record ID", example = "1")
    private Long id;

    @Schema(description = "Short link ID", example = "100")
    @JsonProperty("short_link_id")
    private Long shortLinkId;

    @Schema(description = "Health status", example = "HEALTHY")
    private String status;

    @Schema(description = "Last HTTP status code", example = "200")
    @JsonProperty("last_status_code")
    private Integer lastStatusCode;

    @Schema(description = "Last response time in milliseconds", example = "234")
    @JsonProperty("last_response_time_ms")
    private Long lastResponseTimeMs;

    @Schema(description = "Last error message if any")
    @JsonProperty("last_error")
    private String lastError;

    @Schema(description = "Consecutive failure count", example = "0")
    @JsonProperty("consecutive_failures")
    private Integer consecutiveFailures;

    @Schema(description = "Total health checks performed", example = "150")
    @JsonProperty("check_count")
    private Long checkCount;

    @Schema(description = "Successful health checks", example = "148")
    @JsonProperty("success_count")
    private Long successCount;

    @Schema(description = "Uptime percentage", example = "98.67")
    @JsonProperty("uptime_percentage")
    private Double uptimePercentage;

    @Schema(description = "Last check timestamp")
    @JsonProperty("last_checked_at")
    private LocalDateTime lastCheckedAt;

    @Schema(description = "When monitoring started")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
