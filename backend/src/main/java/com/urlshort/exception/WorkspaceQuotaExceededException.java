package com.urlshort.exception;

/**
 * Exception thrown when a workspace has exceeded its quota or resource limits.
 * Typically results in HTTP 429 Too Many Requests response.
 */
public class WorkspaceQuotaExceededException extends RuntimeException {

    /**
     * Constructs a new WorkspaceQuotaExceededException with the specified detail message.
     *
     * @param message the detail message
     */
    public WorkspaceQuotaExceededException(String message) {
        super(message);
    }

    /**
     * Constructs a new WorkspaceQuotaExceededException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public WorkspaceQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
