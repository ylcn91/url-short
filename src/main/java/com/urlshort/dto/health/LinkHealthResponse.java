package com.urlshort.dto.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Response DTO for link health status.
 */
@Schema(description = "Link health status response")
public record LinkHealthResponse(
        @Schema(description = "Health record ID", example = "1")
        Long id,

        @Schema(description = "Short link ID", example = "100")
        @JsonProperty("short_link_id")
        Long shortLinkId,

        @Schema(description = "Health status", example = "HEALTHY")
        String status,

        @Schema(description = "Last HTTP status code", example = "200")
        @JsonProperty("last_status_code")
        Integer lastStatusCode,

        @Schema(description = "Last response time in milliseconds", example = "234")
        @JsonProperty("last_response_time_ms")
        Long lastResponseTimeMs,

        @Schema(description = "Last error message if any")
        @JsonProperty("last_error")
        String lastError,

        @Schema(description = "Consecutive failure count", example = "0")
        @JsonProperty("consecutive_failures")
        Integer consecutiveFailures,

        @Schema(description = "Total health checks performed", example = "150")
        @JsonProperty("check_count")
        Long checkCount,

        @Schema(description = "Successful health checks", example = "148")
        @JsonProperty("success_count")
        Long successCount,

        @Schema(description = "Uptime percentage", example = "98.67")
        @JsonProperty("uptime_percentage")
        Double uptimePercentage,

        @Schema(description = "Last check timestamp")
        @JsonProperty("last_checked_at")
        LocalDateTime lastCheckedAt,

        @Schema(description = "When monitoring started")
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long shortLinkId;
        private String status;
        private Integer lastStatusCode;
        private Long lastResponseTimeMs;
        private String lastError;
        private Integer consecutiveFailures;
        private Long checkCount;
        private Long successCount;
        private Double uptimePercentage;
        private LocalDateTime lastCheckedAt;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder shortLinkId(Long shortLinkId) { this.shortLinkId = shortLinkId; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder lastStatusCode(Integer lastStatusCode) { this.lastStatusCode = lastStatusCode; return this; }
        public Builder lastResponseTimeMs(Long lastResponseTimeMs) { this.lastResponseTimeMs = lastResponseTimeMs; return this; }
        public Builder lastError(String lastError) { this.lastError = lastError; return this; }
        public Builder consecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; return this; }
        public Builder checkCount(Long checkCount) { this.checkCount = checkCount; return this; }
        public Builder successCount(Long successCount) { this.successCount = successCount; return this; }
        public Builder uptimePercentage(Double uptimePercentage) { this.uptimePercentage = uptimePercentage; return this; }
        public Builder lastCheckedAt(LocalDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public LinkHealthResponse build() { return new LinkHealthResponse(id, shortLinkId, status, lastStatusCode, lastResponseTimeMs, lastError, consecutiveFailures, checkCount, successCount, uptimePercentage, lastCheckedAt, createdAt); }
    }
}
