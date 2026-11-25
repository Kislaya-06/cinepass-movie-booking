package com.example.moviereservation.exception;

/**
 * Thrown when a requested resource (movie, theater, screen, etc.) is not found.
 */
public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
