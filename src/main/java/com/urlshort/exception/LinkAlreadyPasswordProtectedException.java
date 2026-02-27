package com.urlshort.exception;

public class LinkAlreadyPasswordProtectedException extends RuntimeException {

    public LinkAlreadyPasswordProtectedException(String message) {
        super(message);
    }

    public LinkAlreadyPasswordProtectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
