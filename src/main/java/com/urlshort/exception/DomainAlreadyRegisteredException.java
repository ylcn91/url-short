package com.urlshort.exception;

public class DomainAlreadyRegisteredException extends RuntimeException {

    public DomainAlreadyRegisteredException(String message) {
        super(message);
    }

    public DomainAlreadyRegisteredException(String message, Throwable cause) {
        super(message, cause);
    }
}
