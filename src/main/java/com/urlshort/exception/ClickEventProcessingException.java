package com.urlshort.exception;

public class ClickEventProcessingException extends RuntimeException {

    public ClickEventProcessingException(String message) {
        super(message);
    }

    public ClickEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
