package com.urlshort.exception;

import com.urlshort.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Global exception handler for the application.
 * Provides centralized exception handling and converts exceptions to standardized HTTP responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException.
     * Returns HTTP 404 Not Found.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.warn("Resource not found [requestId={}]: {}", requestId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles DuplicateResourceException.
     * Returns HTTP 409 Conflict.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 409 status
     */
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.warn("Duplicate resource [requestId={}]: {}", requestId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("CONFLICT")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles InvalidUrlException.
     * Returns HTTP 400 Bad Request.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(InvalidUrlException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleInvalidUrlException(
            InvalidUrlException ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.warn("Invalid URL [requestId={}]: {}", requestId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_URL")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles LinkExpiredException.
     * Returns HTTP 410 Gone.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 410 status
     */
    @ExceptionHandler(LinkExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ResponseEntity<ErrorResponse> handleLinkExpiredException(
            LinkExpiredException ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.warn("Link expired [requestId={}]: {}", requestId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.GONE.value())
                .error("GONE")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.GONE).body(errorResponse);
    }

    /**
     * Handles WorkspaceQuotaExceededException.
     * Returns HTTP 429 Too Many Requests.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 429 status
     */
    @ExceptionHandler(WorkspaceQuotaExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ResponseEntity<ErrorResponse> handleWorkspaceQuotaExceededException(
            WorkspaceQuotaExceededException ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.warn("Workspace quota exceeded [requestId={}]: {}", requestId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("TOO_MANY_REQUESTS")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    /**
     * Handles UnauthorizedException.
     * Returns HTTP 401 Unauthorized.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 401 status
     */
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.warn("Unauthorized access [requestId={}]: {}", requestId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("UNAUTHORIZED")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles validation errors from @Valid annotation.
     * Returns HTTP 400 Bad Request with field-level errors.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 400 status and validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.warn("Validation error [requestId={}]", requestId);

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                validationErrors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message("Validation failed")
                .path(request.getRequestURI())
                .requestId(requestId)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles malformed JSON or unparseable message body.
     * Returns HTTP 400 Bad Request.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.warn("Malformed request body [requestId={}]", requestId);

        String message = "Invalid request format. Please check your JSON syntax and try again.";

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(message)
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles all other uncaught exceptions.
     * Returns HTTP 500 Internal Server Error.
     * Does not leak sensitive information in error message.
     *
     * @param ex      the exception
     * @param request the HTTP request
     * @return ErrorResponse with 500 status
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {

        String requestId = getOrCreateRequestId();
        log.error("Unexpected error [requestId={}]", requestId, ex);

        String message = "An unexpected error occurred. Please try again later.";

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message(message)
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Retrieves or creates a request ID for tracing purposes.
     * Attempts to get the request ID from the current thread's request context.
     * If not available, generates a new UUID.
     *
     * @return the request ID as a String
     */
    private String getOrCreateRequestId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String existingId = request.getHeader("X-Request-ID");
                if (existingId != null && !existingId.isEmpty()) {
                    return existingId;
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve request context for request ID", e);
        }
        return UUID.randomUUID().toString();
    }
}
