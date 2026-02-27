package com.urlshort.exception;

public class VariantWeightExceededException extends RuntimeException {

    public VariantWeightExceededException(String message) {
        super(message);
    }

    public VariantWeightExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
