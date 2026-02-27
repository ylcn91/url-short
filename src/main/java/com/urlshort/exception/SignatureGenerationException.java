package com.urlshort.exception;

public class SignatureGenerationException extends RuntimeException {

    public SignatureGenerationException(String message) {
        super(message);
    }

    public SignatureGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
