package com.urlshort.exception;

public class DomainNotVerifiedException extends RuntimeException {

    public DomainNotVerifiedException(String message) {
        super(message);
    }

    public DomainNotVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }
}
