package com.example.moviereservation.exception;

/**
 * Base unchecked runtime exception for all domain-specific application errors.
 */
public class ApplicationException extends RuntimeException {

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
