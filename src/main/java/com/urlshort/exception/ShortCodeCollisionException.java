package com.urlshort.exception;

public class ShortCodeCollisionException extends RuntimeException {

    public ShortCodeCollisionException(String message) {
        super(message);
    }

    public ShortCodeCollisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
