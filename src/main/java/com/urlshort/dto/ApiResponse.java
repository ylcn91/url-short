package com.urlshort.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic wrapper for API responses.
 * Provides a consistent response structure across all API endpoints.
 *
 * @param <T> The type of data being returned
 */
@Schema(description = "Generic API response wrapper")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(

        @Schema(description = "Indicates if the request was successful", example = "true")
        Boolean success,

        @Schema(description = "The response data payload")
        T data,

        @Schema(description = "Optional message providing additional context", example = "Short link created successfully")
        String message
) {
    /**
     * Creates a successful response with data.
     *
     * @param data The response data
     * @param <T> The type of data
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * Creates a successful response with data and a message.
     *
     * @param data The response data
     * @param message Success message
     * @param <T> The type of data
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    /**
     * Creates a successful response with only a message.
     *
     * @param message Success message
     * @param <T> The type of data
     * @return ApiResponse with success=true and null data
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, null, message);
    }

    /**
     * Creates a failure response with an error message.
     *
     * @param message Error message
     * @param <T> The type of data
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    /**
     * Creates a failure response with data and an error message.
     *
     * @param data Error details
     * @param message Error message
     * @param <T> The type of data
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(T data, String message) {
        return new ApiResponse<>(false, data, message);
    }

    /**
     * Builder pattern for ApiResponse.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private Boolean success;
        private T data;
        private String message;

        public Builder<T> success(Boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(success, data, message);
        }
    }
}
