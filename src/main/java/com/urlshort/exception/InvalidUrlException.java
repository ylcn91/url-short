package com.urlshort.exception;

/**
 * Exception thrown when a URL is invalid or malformed.
 * Typically results in HTTP 400 Bad Request response.
 */
public class InvalidUrlException extends RuntimeException {

    /**
     * Constructs a new InvalidUrlException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidUrlException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidUrlException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public InvalidUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
