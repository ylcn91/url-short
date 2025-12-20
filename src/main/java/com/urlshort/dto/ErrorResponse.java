package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for error responses.
 * Implemented as an immutable Java record.
 */
@Schema(description = "Error response object")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        @Schema(description = "Timestamp when the error occurred", example = "2025-11-18T10:30:00")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code", example = "400")
        Integer status,

        @Schema(description = "Error type", example = "Bad Request")
        String error,

        @Schema(description = "Error message", example = "Invalid request parameters")
        String message,

        @Schema(description = "Request path that caused the error", example = "/api/v1/links")
        String path,

        @Schema(description = "Validation errors mapped by field name",
                example = "{\"originalUrl\": \"Must be a valid URL\", \"customCode\": \"Custom code already exists\"}")
        @JsonProperty("validation_errors")
        Map<String, String> validationErrors,

        @Schema(description = "Unique request identifier for tracing", example = "550e8400-e29b-41d4-a716-446655440000")
        @JsonProperty("request_id")
        String requestId
) {
    /**
     * Builder pattern for ErrorResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime timestamp;
        private Integer status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> validationErrors;
        private String requestId;

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(Integer status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder validationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(timestamp, status, error, message, path, validationErrors, requestId);
        }
    }
}
