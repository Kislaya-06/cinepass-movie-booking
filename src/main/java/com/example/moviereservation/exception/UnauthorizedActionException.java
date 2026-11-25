package com.example.moviereservation.exception;

/**
 * Thrown when a user attempts an unauthorized modification (e.g. cancelling another user's reservation).
 */
public class UnauthorizedActionException extends ApplicationException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
