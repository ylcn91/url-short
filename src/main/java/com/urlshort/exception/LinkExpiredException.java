package com.urlshort.exception;

/**
 * Exception thrown when attempting to access a link that has expired.
 * Typically results in HTTP 410 Gone response.
 */
public class LinkExpiredException extends RuntimeException {

    /**
     * Constructs a new LinkExpiredException with the specified detail message.
     *
     * @param message the detail message
     */
    public LinkExpiredException(String message) {
        super(message);
    }

    /**
     * Constructs a new LinkExpiredException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public LinkExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
